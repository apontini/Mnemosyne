package ap.mnemosyne.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
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
import android.os.*
import android.os.Bundle
import ap.mnemosyne.http.HttpHelper
import ap.mnemosyne.resources.Message
import ap.mnemosyne.resources.Resource
import ap.mnemosyne.session.SessionHelper
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
import org.jetbrains.anko.doAsync
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat


class HintsService : Service(), LocationListener
{
    companion object {
        const val START = "START"
        const val STOP = "STOP"
        const val CHANNEL_ID = "MNEMOSYNE"
        const val ACTION_ASK_POSITION_PERMISSION = 200
        const val ACTION_ASK_COARSE_POSITION_PERMISSION = 201
        const val ACTION_ACTIVATE_POSITION = 202
        const val ACTION_CHANGE_POSITION_SETTINGS = 203
        const val TEXT_NOTIFICATION_ID = 2
    }
    init {
        Log.d("SERVICE", "Attivato")
        createNotificationChannel()
    }

    lateinit var handler : Handler
    private lateinit var sessionid : String
    private lateinit var googleApiClient : GoogleApiClient
    private lateinit var locationRequest : LocationRequest
    var delayCallback = 0L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
        when(intent?.action ?: START)
        {
            START ->
            {
                Log.d("SERVICE", "Mi avvio...")
                sessionid = getSharedPreferences(getString(R.string.sharedPreferences_user_FILE), Context.MODE_PRIVATE)
                        .getString(getString(R.string.sharedPreferences_user_sessionid), "") ?: ""
                val apiAvailability = GoogleApiAvailability.getInstance()
                val resultCode = apiAvailability.isGooglePlayServicesAvailable(this)

                if(resultCode != ConnectionResult.SUCCESS)
                {
                    createTextNotification(getString(R.string.notification_googleapi_notConnected_title), getString(
                                            R.string.notification_googleapi_notConnected_descr))
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
        }
        return START_STICKY
    }

    private val googleApiConnections = object : GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
    {
        override fun onConnectionFailed(p0: ConnectionResult)
        {
            Log.d("SERVICE", "Errore di collegamento ai servizi Google")
            createTextNotification(getString(R.string.notification_googleapi_notConnected_title), getString(
                R.string.notification_googleapi_notConnected_descr))
            stopHintsService()
        }

        override fun onConnectionSuspended(p0: Int) { }

        override fun onConnected(p0: Bundle?)
        {
            Log.d("SERVICE", "Collegato ai servizi Google")
            startHintsService()
        }
    }

    private fun checkHints() : Long
    {
        return 15000L
    }

    val handlerCallback = object : Runnable {
        @SuppressLint("MissingPermission")
        override fun run()
        {
            checkLocationAndDo{
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this@HintsService)
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
            .addHeader("Cookie" , "JSESSIONID=" + sessionid)
            .url(HttpHelper.HINTS_URL)
            .post(body)
            .build()

        doAsync {
            val response : Pair<Resource?, Response> = HttpHelper(this@HintsService).request(request, true)
            when(response.second.code())
            {
                200->
                {

                }

                401->
                {
                    //TODO chiedere login
                    val respMessage = response.first as Message
                    createTextNotification("Errore", respMessage.message)
                }

                400->
                {
                    val respMessage = response.first as Message
                    createTextNotification("Errore", respMessage.message)
                }

                406->
                {
                    //Per ora non verrà mai restituita
                    val respMessage = response.first as Message
                    createTextNotification("Errore", respMessage.message)
                }

                500 ->
                {
                    val respMessage = response.first as Message
                    createTextNotification("Errore", respMessage.message)
                }
            }
        }
        delayCallback = checkHints()
        handler.postDelayed(handlerCallback, delayCallback)
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
        checkLocationAndDo {
            Log.d("SERVICE", "Ok, inizio il callback per la posizione")
            handler = Handler()
            handler.postDelayed(handlerCallback, delayCallback)
        }
    }

    fun checkLocationAndDo(doWhat : () -> (Unit))
    {
        if(!PermissionsHelper.checkPositionPermission(this@HintsService))
        {
            createPrerequisitesNotification(getString(R.string.alert_noPositionPermission), getString(R.string.notification_permission_description), ACTION_ASK_POSITION_PERMISSION)
            stopHintsService()
            return
        }

        if(!PermissionsHelper.checkCoarsePositionPermission(this@HintsService))
        {
            createPrerequisitesNotification(getString(R.string.alert_noCoarsePositionPermission), getString(R.string.notification_permission_description), ACTION_ASK_COARSE_POSITION_PERMISSION)
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
                createPrerequisitesNotification(getString(R.string.notification_settingsPosition_title), getString(R.string.notification_settingsPosition_descr), ACTION_CHANGE_POSITION_SETTINGS, exception.resolution)
            }
            else
            {
                exception.printStackTrace()
            }
            stopHintsService()
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
            setStyle(bigTextStyle)
            setWhen(System.currentTimeMillis())
            setSmallIcon(R.mipmap.ic_mnemosyne_notif)
            priority = NotificationCompat.PRIORITY_MAX
            val largeIconBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_mnemosyne_launcher)
            setLargeIcon(largeIconBitmap)
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

    fun createTextNotification(title: String, text : String)
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
            setSmallIcon(R.mipmap.ic_mnemosyne_notif)
            val largeIconBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_mnemosyne_launcher)
            setLargeIcon(largeIconBitmap)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setChannelId(CHANNEL_ID)
        }

        with(getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager) {
            notify(TEXT_NOTIFICATION_ID, notification.build())
        }
    }

    fun stopHintsService()
    {
        Log.d("SERVICE","Mi fermo..")
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(TEXT_NOTIFICATION_ID)
        try { handler.removeCallbacks(handlerCallback) } catch (ex : UninitializedPropertyAccessException) {}
        try{LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this@HintsService)} catch (ise : IllegalStateException){}
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