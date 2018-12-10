package ap.mnemosyne.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
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
    private var locationManager : LocationManager? = null
    private lateinit var googleApiClient : GoogleApiClient
    private lateinit var locationRequest : LocationRequest
    var delayCallback = 0L

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int
    {
        when(intent.action)
        {
            START ->
            {
                Log.d("SERVICE", "Mi avvio...")
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
        return super.onStartCommand(intent, flags, startId)
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
        return 10000L
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
        Log.d("SERVICE", "Trovata posizione")
        createTextNotification("Posizione", "${p0?.latitude ?: "null"} , ${p0?.longitude ?: "null"}")
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this)
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
            addAction(R.drawable.places_ic_clear, "Kill me (for debug purposes)", killPendingIntent)
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
        }

        if(!PermissionsHelper.checkCoarsePositionPermission(this@HintsService))
        {
            createPrerequisitesNotification(getString(R.string.alert_noCoarsePositionPermission), getString(R.string.notification_permission_description), ACTION_ASK_COARSE_POSITION_PERMISSION)
            stopHintsService()
        }

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if(locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == false)
        {
            createPrerequisitesNotification(getString(R.string.notification_noPositionServices_title), getString(R.string.notification_noPositionServices_descr), ACTION_ACTIVATE_POSITION)
            stopHintsService()
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
                createPrerequisitesNotification(getString(R.string.notification_settingsPosition_title), getString(R.string.notification_settingsPosition_descr), ACTION_CHANGE_POSITION_SETTINGS)
            }
            else
            {
                exception.printStackTrace()
            }
            stopHintsService()
        }
    }

    fun createPrerequisitesNotification(title: String, text : String, action : Int)
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
            val largeIconBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_mnemosyne_launcher)
            setLargeIcon(largeIconBitmap)
            setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            val clickInt = Intent(this@HintsService, ServiceActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("ACTION", action)
            }

            val pendingClickIntent = PendingIntent.getActivity(this@HintsService, 0, clickInt, PendingIntent.FLAG_UPDATE_CURRENT)

            setContentIntent(pendingClickIntent)
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
            setWhen(System.currentTimeMillis())
            setAutoCancel(true)
            setSmallIcon(R.mipmap.ic_mnemosyne_notif)
            val largeIconBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_mnemosyne_launcher)
            setLargeIcon(largeIconBitmap)
            setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
        }

        with(getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager) {
            notify(TEXT_NOTIFICATION_ID, notification.build())
        }
    }

    fun stopHintsService()
    {
        Log.d("SERVICE","Mi fermo..")
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
        try { handler.removeCallbacks(handlerCallback) } catch (ex : UninitializedPropertyAccessException) {}
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this@HintsService)
        try { googleApiClient.disconnect() } catch (ex : UninitializedPropertyAccessException) {}
        stopForeground(true)
        stopSelf()
    }

    private fun createNotificationChannel()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = CHANNEL_ID
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
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