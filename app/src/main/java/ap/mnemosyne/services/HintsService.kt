package ap.mnemosyne.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.util.Log
import android.graphics.BitmapFactory
import android.location.Location
import androidx.core.app.NotificationCompat
import ap.mnemosyne.R
import ap.mnemosyne.activities.ServiceActivity
import ap.mnemosyne.activities.SplashActivity
import ap.mnemosyne.permissions.PermissionsHelper
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import java.lang.IllegalStateException
import android.media.RingtoneManager
import android.net.wifi.WifiManager
import android.os.*
import android.os.Bundle
import ap.mnemosyne.activities.TaskDetailsActivity
import ap.mnemosyne.http.HttpHelper
import ap.mnemosyne.receivers.AlarmReceiver
import ap.mnemosyne.resources.*
import ap.mnemosyne.resources.Message
import ap.mnemosyne.session.SessionHelper
import ap.mnemosyne.tasks.TasksHelper
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
import org.jetbrains.anko.doAsync
import org.joda.time.LocalDateTime
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat
import java.lang.StringBuilder


class HintsService : Service(), LocationListener
{
    companion object {
        const val START = "START"
        const val STOP = "STOP"
        const val CHECK_HINTS = "CHECK_HINTS"
        const val CHANNEL_ID = "MNEMOSYNE"
        const val ACTION_ASK_POSITION_PERMISSION = 200
        const val ACTION_ASK_COARSE_POSITION_PERMISSION = 201
        const val ACTION_ACTIVATE_POSITION = 202
        const val ACTION_CHANGE_POSITION_SETTINGS = 203
        const val TEXT_NOTIFICATION_ID = 2
        const val TEXT_ERROR_ID = 3
    }

    init {
        Log.d("SERVICE", "Attivato")
    }

    private lateinit var tasks : TasksHelper
    private lateinit var session : SessionHelper
    private lateinit var snoozed : SnoozeHelper
    private lateinit var googleApiClient : GoogleApiClient
    private lateinit var locationRequest : LocationRequest
    private lateinit var wakeLock : PowerManager.WakeLock
    private lateinit var wifiLock : WifiManager.WifiLock
    private lateinit var alarmMgr: AlarmManager
    private lateinit var alarmIntent : PendingIntent
    private var alarmDelay = 10000L

    override fun onCreate()
    {
        tasks = TasksHelper(this)
        snoozed = SnoozeHelper(this)
        session = SessionHelper(this)
        createNotificationChannel()

        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag")
            }

        wifiLock = (applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).createWifiLock(WifiManager.WIFI_MODE_FULL, "mnemosyne_wifi_lock")
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
        when(intent?.action ?: START)
        {
            START ->
            {
                Log.d("SERVICE", "Mi avvio...")
                val apiAvailability = GoogleApiAvailability.getInstance()
                val resultCode = apiAvailability.isGooglePlayServicesAvailable(this)

                if(resultCode != ConnectionResult.SUCCESS)
                {
                    createTextNotification(getString(R.string.notification_googleapi_notConnected_title), getString(
                                            R.string.notification_googleapi_notConnected_descr), true)
                    stopHintsService()
                }
                else
                {
                    Log.d("SERVICE", "Cerco di collegarmi ai servizi google.. ")

                    googleApiClient = GoogleApiClient.Builder(this).addApi(LocationServices.API).addConnectionCallbacks(googleApiConnections)
                       .addOnConnectionFailedListener(googleApiConnections).build()
                    googleApiClient.connect()
                }
            }

            STOP ->
            {
                stopHintsService()
            }

            CHECK_HINTS ->
            {
                try
                {
                    if(!googleApiClient.isConnected)
                    {
                        Log.d("SERVICE", "Called CHECK_HINTS but Google API client is not connected")
                    }
                    else
                    {
                        checkLocationAndDo {
                            Log.d("SERVICE", "Inizio calcolo hints")
                            startCheck()
                        }
                    }
                }
                catch (un : UninitializedPropertyAccessException)
                {
                    Log.d("SERVICE", "Called CHECK_HINTS but START was not called")
                }
            }
        }
        return START_STICKY
    }

    private val googleApiConnections = object : GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
    {
        override fun onConnectionFailed(p0: ConnectionResult)
        {
            Log.d("SERVICE", "Errore di collegamento ai servizi Google")
            createTextNotification(getString(R.string.notification_googleapi_notConnected_title), getString(
                R.string.notification_googleapi_notConnected_descr), true)
            stopHintsService()
        }

        override fun onConnectionSuspended(p0: Int) { }

        override fun onConnected(p0: Bundle?)
        {
            Log.d("SERVICE", "Collegato ai servizi Google")
            startHintsService()
        }
    }

    private fun calculateCallbackTime(pos : Location?) : Long
    {
        return 180000L
    }

    @SuppressLint("MissingPermission")
    fun startCheck()
    {
        wakeLock.acquire(180000L)
        wifiLock.acquire()
        checkLocationAndDo{
            val lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient)
            if(lastLocation != null && System.currentTimeMillis() - lastLocation.time > 30000)
            {
                Log.d("SERVICE", "Calcolo una nuova posizione")
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,
                    locationRequest,
                    this@HintsService)
            }
            else
            {
                Log.d("SERVICE", "Uso l'ultima posizione")
                onLocationChanged(lastLocation)
            }
        }
    }

    override fun onLocationChanged(p0: Location?)
    {
        Log.d("SERVICE", "Trovata posizione: ${p0?.latitude ?: "null"} , ${p0?.longitude ?: "null"}, velocità: ${p0?.speed}")
        //createTextNotification("Posizione", "${p0?.latitude ?: "null"} , ${p0?.longitude ?: "null"}, velocità: ${p0?.speed}")
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this)
        val time = LocalTime.now().toString(DateTimeFormat.forPattern("HH:mm"))
        val body = FormBody.Builder().add("lat", p0?.latitude.toString()).add("lon", p0?.longitude.toString())
            .add("time", time).build()

        val request = Request.Builder()
            .addHeader("Cookie" , "JSESSIONID=" + session.user.sessionID)
            .url(HttpHelper.HINTS_URL)
            .post(body)
            .build()

        doAsync {
            val response : Pair<Resource?, Response> = HttpHelper(this@HintsService).request(request, true)
            when(response.second.code())
            {
                200->
                {
                    val hints = (response.first as ResourceList<Hint>).list as List<Hint>
                    with(getSharedPreferences(getString(R.string.sharedPreferences_tasks_FILE),Context.MODE_PRIVATE).edit())
                    {
                        putString(getString(R.string.sharedPreferences_tasks_hints), (response.first as ResourceList<Hint>).toJSON())
                        putString(getString(R.string.sharedPreferences_tasks_hints_lastRefresh), LocalDateTime.now().toString(TasksHelper.dateTimeFormat))
                        apply()
                    }
                    var notfound = 0
                    hints.forEach {
                        if(tasks.getLocalTask(it.taskID) == null)
                        {
                            notfound++
                        }
                    }
                    if(notfound>0)
                    {
                        tasks.updateTasksAndDo(true) {
                            handleTasksNotification(hints)
                        }
                    }
                    else
                    {
                        handleTasksNotification(hints)
                    }
                }

                401->
                {
                    val respMessage = response.first as Message
                    createTextNotification("Errore", "${respMessage.message}, tocca per risolvere", true)
                    stopHintsService()
                }

                400->
                {
                    val respMessage = response.first as Message
                    createTextNotification("Errore", respMessage.message, true)
                }

                406->
                {
                    //Per ora non verrà mai restituita
                    val respMessage = response.first as Message
                    createTextNotification("Errore", respMessage.message, true)
                }

                500 ->
                {
                    val respMessage = response.first as Message
                    createTextNotification("Errore", respMessage.message, true)
                }
            }

            alarmDelay = calculateCallbackTime(p0)

            alarmMgr.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + alarmDelay,
                alarmIntent)
            if(wifiLock.isHeld) wifiLock.release()
            if(wakeLock.isHeld) wakeLock.release()
        }
    }

    fun startHintsService()
    {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID).apply {
            val bigTextStyle = NotificationCompat.BigTextStyle()
            with(bigTextStyle) {
                setBigContentTitle(getString(R.string.service_title))
                bigText(getString(R.string.service_descr))
            }
            setContentTitle(getString(R.string.service_title))
            setStyle(bigTextStyle)
            setWhen(System.currentTimeMillis())
            setSmallIcon(R.mipmap.ic_mnemosyne_notif)
            val largeIconBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_mnemosyne_launcher)
            setLargeIcon(largeIconBitmap)
            setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            val clickInt = Intent(this@HintsService, SplashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingClickIntent: PendingIntent = TaskStackBuilder.create(this@HintsService).run {
                addNextIntentWithParentStack(clickInt)
                getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
            }
            setContentIntent(pendingClickIntent)

            val killIntent = Intent(this@HintsService, HintsService::class.java).apply {
                action = STOP
            }
            val killPendingIntent: PendingIntent =
                PendingIntent.getService(this@HintsService, 9, killIntent, 0)
            addAction(R.drawable.places_ic_clear, "Non avvisarmi più", killPendingIntent)
        }

        startForeground(1, notification.build())
        alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmIntent = Intent(this, AlarmReceiver::class.java).let { intent ->
            PendingIntent.getBroadcast(this, 0, intent, 0)
        }

        alarmMgr.setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + alarmDelay,
            alarmIntent)
    }

    fun checkLocationAndDo(doWhat : () -> (Unit))
    {
        if(!PermissionsHelper.checkPositionPermission(this@HintsService))
        {
            createPrerequisitesNotification(getString(R.string.alert_noPositionPermission), getString(R.string.notification_permission_description), ACTION_ASK_POSITION_PERMISSION)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(
                ACTION_ASK_POSITION_PERMISSION)
            stopHintsService()
            return
        }

        if(!PermissionsHelper.checkCoarsePositionPermission(this@HintsService))
        {
            createPrerequisitesNotification(getString(R.string.alert_noCoarsePositionPermission), getString(R.string.notification_permission_description), ACTION_ASK_COARSE_POSITION_PERMISSION)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(
                ACTION_ASK_COARSE_POSITION_PERMISSION)
            stopHintsService()
            return
        }

        locationRequest = LocationRequest().apply {
            interval = 1000
            fastestInterval = 0
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(this@HintsService)
        val task: com.google.android.gms.tasks.Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            doWhat()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException)
            {
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(
                    ACTION_CHANGE_POSITION_SETTINGS)
                createPrerequisitesNotification(getString(R.string.notification_settingsPosition_title), getString(R.string.notification_settingsPosition_descr), ACTION_CHANGE_POSITION_SETTINGS, exception.resolution)
            }
            else
            {
                exception.printStackTrace()
            }
            stopHintsService()
        }
    }

    fun handleTasksNotification(hints : List<Hint>)
    {

        hints.forEach {
            val t : Task? = tasks.getLocalTask(it.taskID)
            //If task is null, it means that after the service asked for hints and BEFORE their processing, task was deleted
            if(t!=null)
            {
                if (!snoozed.isSnoozed(t.id) || it.isUrgent)
                {
                    snoozed.unSnooze(t.id) //To free local space up removing it from the map
                    val builder = StringBuilder().apply {
                        if(it.closestPlace?.name != null) append(it.closestPlace?.name + ", ")
                        if(it.closestPlace?.town != null)
                        {
                            append(it.closestPlace?.town)
                            if(it.closestPlace?.suburb != null) append(" (" + it.closestPlace?.suburb + ")")
                            append(", ")
                        }

                        if(it.closestPlace?.road != null) append(it.closestPlace?.road + ", ")
                        trim()
                        if(length>0) deleteCharAt(length-2)
                        else append("Tocca per visualizzare il posto più vicino")
                    }
                    createTaskNotification(t.name.capitalize(), builder.toString(), it)
                }
            }
        }
    }

    fun createPrerequisitesNotification(title: String, text : String, action : Int, exc : PendingIntent? = null)
    {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID).apply {
            val bigTextStyle = NotificationCompat.BigTextStyle()
            with(bigTextStyle) {
                setBigContentTitle(title)
                bigText(text)
            }
            setContentText(text)
            setContentTitle(title)
            setStyle(bigTextStyle)
            setWhen(System.currentTimeMillis())
            setSmallIcon(R.mipmap.ic_mnemosyne_notif)
            priority = NotificationCompat.PRIORITY_MAX
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            setSound(uri)
            setVibrate(LongArray(1) {1000L})
            val clickInt = Intent(this@HintsService, ServiceActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("ACTION", action)
                if(exc != null)
                {
                    val bundle = Bundle()
                    bundle.putParcelable("PIntent", exc)
                    putExtras(bundle)
                }
            }
            val pendingClickIntent = PendingIntent.getActivity(this@HintsService, 0, clickInt, PendingIntent.FLAG_UPDATE_CURRENT)
            setContentIntent(pendingClickIntent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setChannelId(CHANNEL_ID)
        }

        with(getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager) {
            notify(action, notification.build())
        }
    }

    fun createTaskNotification(title: String, text : String, hint : Hint)
    {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID).apply {
            val bigTextStyle = NotificationCompat.BigTextStyle()
            with(bigTextStyle) {
                setBigContentTitle(title)
                bigText(text)
            }
            setStyle(bigTextStyle)
            setContentTitle(title)
            setContentText(text)
            setWhen(System.currentTimeMillis())
            setSmallIcon(R.mipmap.ic_mnemosyne_notif)
            priority = NotificationCompat.PRIORITY_MAX
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            setSound(uri)

            val clickInt = Intent(this@HintsService, TaskDetailsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("task", hint.taskID)
                putExtra("focusPlace", hint.closestPlace)
            }
            val pendingClickIntent = PendingIntent.getActivity(this@HintsService, hint.taskID, clickInt, PendingIntent.FLAG_UPDATE_CURRENT)
            setContentIntent(pendingClickIntent)

            when
            {
                hint.isConfirm ->
                {
                    with(bigTextStyle) {
                        setBigContentTitle(title)
                        bigText(getString(R.string.notification_completedQuestion))
                    }
                    setStyle(bigTextStyle)
                    setContentText(getString(R.string.notification_completedQuestion))
                    setVibrate(LongArray(1) {1000L})
                    setOngoing(true)
                    val successIntent = Intent(this@HintsService, HintsHelperService::class.java).apply {
                        action = HintsHelperService.ACTION_COMPLETED_SUCCESS
                        putExtra("taskID", hint.taskID)
                    }
                    val successPendingIntent = PendingIntent.getService(this@HintsService, hint.taskID, successIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                    addAction(R.drawable.ic_baseline_check_24px, "Sì", successPendingIntent)

                    val failedIntent = Intent(this@HintsService, HintsHelperService::class.java).apply {
                        action = HintsHelperService.ACTION_COMPLETED_FAILED
                        putExtra("taskID", hint.taskID)
                    }
                    val failedPendingIntent = PendingIntent.getService(this@HintsService, hint.taskID, failedIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                    addAction(R.drawable.ic_baseline_clear_24px, "No", failedPendingIntent)
                }

                hint.isUrgent ->
                {
                    with(bigTextStyle) {
                        setBigContentTitle("URGENTE! ${title.capitalize()}")
                        bigText(text)
                    }
                    setStyle(bigTextStyle)
                    setContentTitle("URGENTE! ${title.capitalize()}")
                    setVibrate(LongArray(1) {2000L})
                    setOngoing(true)
                }

                else ->
                {
                    setVibrate(LongArray(1) {1000L})
                    val snoozeIntent = Intent(this@HintsService, HintsHelperService::class.java).apply {
                        action = HintsHelperService.ACTION_SNOOZE_MIN
                        putExtra("taskID", hint.taskID)
                    }
                    val snoozePendingIntent = PendingIntent.getService(this@HintsService, hint.taskID, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                    addAction(R.drawable.ic_baseline_clear_24px, "Ritarda (15 min)", snoozePendingIntent)

                    val snoozeMaxIntent = Intent(this@HintsService, HintsHelperService::class.java).apply {
                        action = HintsHelperService.ACTION_SNOOZE_MAX
                        putExtra("taskID", hint.taskID)
                    }
                    val snoozeMaxPendingIntent = PendingIntent.getService(this@HintsService, hint.taskID, snoozeMaxIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                    addAction(R.drawable.ic_baseline_clear_24px, "Ritarda (1 ora)", snoozeMaxPendingIntent)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setChannelId(CHANNEL_ID)
        }

        with(getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager) {
            notify(hint.taskID, notification.build())
        }
    }

    fun createTextNotification(title: String, text : String, isError : Boolean = false)
    {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setContentTitle(title)
            setContentText(text)
            val bigTextStyle = NotificationCompat.BigTextStyle()
            with(bigTextStyle) {
                setBigContentTitle(title)
                bigText(text)
            }
            setStyle(bigTextStyle)
            setWhen(System.currentTimeMillis())
            setVibrate(LongArray(1) {1000L})
            priority = NotificationCompat.PRIORITY_MAX
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            setSound(uri)

            val clickInt = Intent(this@HintsService, SplashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingClickIntent: PendingIntent = TaskStackBuilder.create(this@HintsService).run {
                addNextIntentWithParentStack(clickInt)
                getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
            }
            setContentIntent(pendingClickIntent)

            setSmallIcon(R.mipmap.ic_mnemosyne_notif)
            val largeIconBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_mnemosyne_launcher)
            setLargeIcon(largeIconBitmap)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setChannelId(CHANNEL_ID)
        }

        with(getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager) {
            notify(
                if(!isError) TEXT_NOTIFICATION_ID else TEXT_ERROR_ID,
                notification.build())
        }
    }

    fun stopHintsService()
    {
        Log.d("SERVICE","Mi fermo..")
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(TEXT_NOTIFICATION_ID)
        try { alarmMgr.cancel(alarmIntent) } catch (ex : UninitializedPropertyAccessException) {}
        try{LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this@HintsService)} catch (ise : IllegalStateException){} catch (ex : UninitializedPropertyAccessException) {}
        try { googleApiClient.disconnect() } catch (ex : UninitializedPropertyAccessException) {}
        stopForeground(true)
        stopSelf()
    }

    private fun createNotificationChannel()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = CHANNEL_ID
            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH)
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent): IBinder?
    {
        return null
    }

}