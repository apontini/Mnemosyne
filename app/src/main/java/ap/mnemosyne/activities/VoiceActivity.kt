package ap.mnemosyne.activities

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity;
import ap.mnemosyne.R

import kotlinx.android.synthetic.main.activity_voice.*
import android.speech.RecognizerIntent
import android.content.Intent
import android.content.IntentSender
import android.graphics.drawable.AnimatedVectorDrawable
import android.location.LocationManager
import android.util.Log
import android.view.MenuItem
import android.view.View
import ap.mnemosyne.enums.ParamsName
import ap.mnemosyne.http.HttpHelper
import ap.mnemosyne.permissions.PermissionsHelper
import ap.mnemosyne.resources.*
import ap.mnemosyne.session.SessionHelper
import ap.mnemosyne.tasks.TasksHelper
import com.google.android.gms.location.places.ui.PlacePicker
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.content_voice.*
import okhttp3.*
import org.jetbrains.anko.*
import org.jetbrains.anko.design.snackbar
import org.joda.time.LocalTime
import java.util.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.Animatable2
import android.location.Location
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.location.*
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResolvableApiException
import kotlinx.android.synthetic.main.drawer_header.view.*
import org.jetbrains.anko.design.longSnackbar


class VoiceActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, LocationListener
{
    private lateinit var session : SessionHelper
    private lateinit var tasks : TasksHelper
    private var locationManager : LocationManager? = null
    private lateinit var googleApiClient : GoogleApiClient
    private lateinit var anim : AnimatedVectorDrawable
    private lateinit var callback : Animatable2.AnimationCallback
    private lateinit var locationRequest : LocationRequest

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        session = SessionHelper(this)
        tasks = TasksHelper(this)
        setContentView(R.layout.activity_voice)
        setSupportActionBar(toolbar)
        setNavDrawer()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val sessionid = session.user.sessionID

        if(sessionid == "")
        {
            alert(getString(R.string.alert_noSession)){
                okButton { finish() }
            }.show()
            return
        }

        if(!PermissionsHelper.checkMicrophonePermission(this))
        {
            alert(getString(R.string.alert_noRecordAudioPermission)){
                okButton { finish() }
            }.show()
            return
        }

        val pm = packageManager
        val activities = pm.queryIntentActivities(Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0)
        if (activities.size != 0)
        {
            startVoiceRecognitionActivity()
        }
        else
        {
            alert(getString(R.string.alert_voice_vttNotPossible)) {}.show()
        }

        saveTaskButton.setOnClickListener {
            if(textSentence.text.toString() != "")
                sendTask(sessionid)
        }

        chip1.setOnClickListener { onChipClick(it) }
        chip2.setOnClickListener { onChipClick(it) }
        chip3.setOnClickListener { onChipClick(it) }
        chip4.setOnClickListener { onChipClick(it) }
    }

    private fun onChipClick(chip : View)
    {
        textSentence.text?.clear()
        textSentence.setText((chip as Chip).text)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        return when (item.itemId)
        {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy()
    {
        try
        {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this)
            googleApiClient.disconnect()
        }
        catch (ue : UninitializedPropertyAccessException){}
        super.onDestroy()
    }

    private fun sendTask(sessionid : String)
    {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(this)

        if(resultCode != ConnectionResult.SUCCESS)
        {
            textStatus.text = getString(R.string.alert_noGoogleAPI)
            return
        }

        googleApiClient = GoogleApiClient.Builder(this).addApi(LocationServices.API).addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this).build()
        googleApiClient.connect()
    }

    override fun onConnected(p0: Bundle?)
    {

        if(!PermissionsHelper.checkPositionPermission(this))
        {
            alert(getString(R.string.alert_noPositionPermission)){
                okButton {
                    finish()
                    googleApiClient.disconnect()
                }
            }.show()
            return
        }

        if(!PermissionsHelper.checkCoarsePositionPermission(this))
        {
            alert(getString(R.string.alert_noCoarsePositionPermission)){
                okButton {
                    finish()
                    googleApiClient.disconnect()
                }
            }.show()
            return
        }

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if(locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == false)
        {
            alert("I servizi di localizzazione non sono abilitati o sono impostati in risparmio energia!").show()
            googleApiClient.disconnect()
            return
        }

        locationRequest = LocationRequest().apply {
            interval = 1000
            fastestInterval = 0
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: com.google.android.gms.tasks.Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            val location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient)

            toolbar.longSnackbar("Last location: $location")

            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this)

            progressBar.visibility = View.VISIBLE
            progressStatus.visibility = View.VISIBLE
            progressStatus.text = getString(R.string.text_voice_waitPosition)
            voiceMain.visibility = View.GONE
            voiceAnim.visibility = View.VISIBLE
            anim = voiceAnimImage.drawable as AnimatedVectorDrawable
            callback = object : Animatable2.AnimationCallback()
            {
                override fun onAnimationEnd(drawable: Drawable)
                {
                    anim.start()
                }
            }

            anim.registerAnimationCallback(callback)
            anim.start()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException)
            {
                alert("Per favore, imposta la localizzazione su \"Alta precisione\", altrimenti l'applicazione non funzionerà")
                {
                    yesButton {
                        try
                        {
                            exception.startResolutionForResult(this@VoiceActivity,1235)
                        }
                        catch (sendEx: IntentSender.SendIntentException) { }
                    }
                    noButton {  }
                }.show()
            }
            else
            {
                exception.printStackTrace()
            }
        }
    }

    private fun setNavDrawer()
    {
        nav_view.getHeaderView(0).header_text.text = session.user.email
        nav_view.setNavigationItemSelectedListener { menuItem ->

            when(menuItem.itemId)
            {
                R.id.task_gallery ->
                {
                    val intent = Intent(this, TaskListActivity::class.java)
                    this.startActivityForResult(intent, 101)
                }

                R.id.create_task_manual ->
                {
                    toolbar.snackbar("Non implementato").show()
                }
            }
            drawer_layout.closeDrawers()
            true
        }
    }

    override fun onLocationChanged(p0: Location?)
    {
        Log.d("POSITION", "results: ${p0.toString()}")
        p0 ?: return
        toolbar.snackbar("Posizione acc: ${p0.accuracy}")
        if(p0.accuracy > 1000f) return
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this)
        progressStatus.text = getString(R.string.text_voice_waitServer)
        val body = FormBody.Builder().add("sentence", textSentence.text.toString()).add("lat", p0.latitude.toString())
            .add("lon", p0.longitude.toString()).build()

        val request = Request.Builder()
            .addHeader("Cookie" , "JSESSIONID=" + session.user.sessionID)
            .url(HttpHelper.PARSE_URL)
            .post(body)
            .build()

        doAsync {
            val response : Pair<Resource?, Response> = HttpHelper(this@VoiceActivity).request(request, true)
            var error = true
            when(response.second.code())
            {
                200->{
                    error = false
                    tasks.updateTasksAndDo(true){
                        val returnIntent = Intent()
                        returnIntent.putExtra("resultTask", response.first as Task)
                        setResult(Activity.RESULT_OK, returnIntent)
                        finish()
                    }
                }

                401->{
                    Log.d("SESSION", "Sessione scaduta")
                    val intent = Intent(this@VoiceActivity, LoginActivity::class.java)
                    startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
                }

                400->{
                    uiThread {
                        val respMessage = response.first as Message
                        when(respMessage.errorCode)
                        {
                            "PRSR01" -> {
                                textStatus.text = getString(R.string.text_voice_PRSR01)
                            }

                            "PRSR02" -> {
                                textStatus.text = getString(R.string.text_voice_PRSR02)
                            }

                            "PRSR07" -> {
                                textStatus.text = getString(R.string.text_voice_PRSR07, respMessage.errorDetails.split(":")[1])
                            }

                            "PRSR10" -> {
                                textStatus.text = getString(R.string.text_voice_PRSR10)
                            }

                            else -> {
                                textStatus.text = getString(R.string.text_general_error, respMessage.errorCode + ": " + respMessage.errorDetails)
                            }
                        }
                    }
                }

                404->{
                    uiThread {
                        val respMessage = response.first as Message
                        when(respMessage.errorCode)
                        {
                            "PRSR05" -> {
                                textStatus.text = getString(R.string.text_voice_PRSR05, respMessage.errorDetails.split(":")[1])
                            }

                            "PRSR11" -> {
                                textStatus.text = getString(R.string.text_voice_PRSR11Repeat)
                                alert(getString(R.string.text_voice_PRSR11, respMessage.errorDetails)){
                                    yesButton {
                                        saveTaskButton.isEnabled = false
                                        askParameter(ParamsName.valueOf(respMessage.errorDetails))
                                    }
                                }.show()
                            }

                            else -> {
                                textStatus.text = getString(R.string.text_general_error, respMessage.errorCode + ": " + respMessage.errorDetails)
                            }
                        }
                    }
                }

                500->{
                    uiThread {
                        val respMessage = response.first as Message
                        when(respMessage.errorCode)
                        {
                            "PRSR03" -> {
                                textStatus.text = getString(R.string.text_voice_PRSR03)
                            }

                            "PRSR08" -> {
                                textStatus.text = "SQLException: ${respMessage.errorDetails}"
                            }

                            "PRSR09" -> {
                                textStatus.text = "ServletException: ${respMessage.errorDetails}"
                            }

                            else -> {
                                textStatus.text = getString(R.string.text_general_error, respMessage.errorCode + ": " + respMessage.errorDetails)
                            }
                        }
                    }
                }

                501->{
                    uiThread {
                        val respMessage = response.first as Message
                        when(respMessage.errorCode)
                        {
                            "PRSR04" -> {
                                textStatus.text = getString(R.string.text_voice_PRSR04, respMessage.errorDetails.split(":")[1])
                            }

                            "PRSR06" -> {
                                textStatus.text = getString(R.string.text_voice_PRSR06, respMessage.errorDetails.split(":")[1])
                            }

                            "PRSR13" -> {
                                textStatus.text = getString(R.string.text_voice_PRSR13, respMessage.errorDetails.split(":")[1])
                            }

                            else -> {
                                textStatus.text = getString(R.string.text_general_error, respMessage.errorCode + ": " + respMessage.errorDetails)
                            }
                        }

                    }
                }

                HttpHelper.ERROR_PERMISSIONS->{
                    uiThread {
                        textStatus.text =  getString(R.string.text_general_error, getString(R.string.alert_noInternetPermission))
                    }
                }

                HttpHelper.ERROR_NO_CONNECTION->{
                    uiThread {
                        textStatus.text =  getString(R.string.text_general_error, getString(R.string.alert_noInternetConnection))
                    }
                }

                else ->
                {
                    val respMessage = response.first as Message
                    textStatus.text =  getString(R.string.text_general_error, respMessage.errorCode + ": " + respMessage.errorDetails)
                }
            }
            if(error)
            {
                uiThread {
                    anim.unregisterAnimationCallback(callback)
                    anim.stop()
                    voiceMain.visibility = View.VISIBLE
                    voiceAnim.visibility = View.GONE
                    progressBar.visibility = View.GONE
                    progressStatus.visibility = View.GONE
                }
            }
        }
    }

    private fun askParameter(param : ParamsName)
    {
        when(param)
        {

            ParamsName.location_house ->
            {
                val builder: PlacePicker.IntentBuilder = PlacePicker.IntentBuilder()
                startActivityForResult(builder.build(this@VoiceActivity), 0)
            }

            ParamsName.location_work ->
            {
                val builder: PlacePicker.IntentBuilder = PlacePicker.IntentBuilder()
                startActivityForResult(builder.build(this@VoiceActivity), 1)
            }

            ParamsName.time_lunch ->
            {
                val intent = Intent(this@VoiceActivity, TimeIntervalPickerActivity::class.java)
                startActivityForResult(intent, 2)
            }

            ParamsName.time_bed ->
            {
                val intent = Intent(this@VoiceActivity, TimeIntervalPickerActivity::class.java)
                startActivityForResult(intent, 3)
            }

            ParamsName.time_dinner ->
            {
                val intent = Intent(this@VoiceActivity, TimeIntervalPickerActivity::class.java)
                startActivityForResult(intent, 4)
            }

            ParamsName.time_work ->
            {
                val intent = Intent(this@VoiceActivity, TimeIntervalPickerActivity::class.java)
                startActivityForResult(intent, 5)
            }
        }
    }

    override fun onConnectionFailed(p0: ConnectionResult) {

    }

    override fun onConnectionSuspended(p0: Int) {

    }

    private fun startVoiceRecognitionActivity()
    {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
            "Dimmi cosa vuoi che ti ricordi")
        startActivityForResult(intent, 1234)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode)
        {
            0,1,2,3,4,5 ->
            {
                val paramName = when (requestCode)
                {
                    0 -> ParamsName.location_house
                    1 -> ParamsName.location_work
                    2 -> ParamsName.time_lunch
                    3 -> ParamsName.time_dinner
                    4 -> ParamsName.time_bed
                    5 -> ParamsName.time_work
                    else -> null
                }

                if(paramName != null && resultCode == Activity.RESULT_OK)
                {
                    val bRequest = Request.Builder()
                        .addHeader("Cookie" , "JSESSIONID="+session.user.sessionID)
                        .url(HttpHelper.REST_PARAMETER_URL)

                    when (requestCode)
                    {
                        0, 1 ->
                        {
                            val place = PlacePicker.getPlace(this@VoiceActivity, data)
                            Log.d("PARAMETER", "Creating parameter")
                            val param = LocationParameter(paramName, session.user.email, Point(place.latLng.latitude, place.latLng.longitude), -1, null)
                            bRequest.post(RequestBody.create(MediaType.parse("application/json"), param.toJSON()))
                        }

                        2,3,4,5 ->
                        {
                            val fromTime = data?.getSerializableExtra("fromTime") as LocalTime
                            val toTime = data?.getSerializableExtra("toTime") as LocalTime
                            Log.d("PARAMETER", "Creating parameter")
                            val param = TimeParameter(paramName, session.user.email, fromTime, toTime)
                            bRequest.post(RequestBody.create(MediaType.parse("application/json"), param.toJSON()))
                        }
                    }

                    doAsync {
                        val resp = HttpHelper(this@VoiceActivity).request(bRequest.build(), true)
                        when(resp.second.code())
                        {
                            201, 200 ->
                            {
                                toolbar.snackbar(getString(R.string.text_settings_successUpdate)).show()
                                when (requestCode)
                                {
                                    0, 1 ->
                                    {
                                        with(defaultSharedPreferences.edit())
                                        {
                                            putString(paramName.name, (resp.first as LocationParameter).toJSON())
                                            apply()
                                        }
                                    }

                                    2,3,4,5 ->
                                    {
                                        with(defaultSharedPreferences.edit())
                                        {
                                            putString(paramName.name, (resp.first as LocationParameter).toJSON())
                                            apply()
                                        }
                                    }
                                }
                            }

                            401 ->
                            {
                                Log.d("SESSION", "Sessione scaduta")
                                val intent = Intent(this@VoiceActivity, LoginActivity::class.java)
                                startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
                            }

                            400 ->
                            {
                                uiThread {
                                    alert("400: " + (resp.first as Message).errorDetails).show()
                                }
                            }

                            500 ->
                            {
                                uiThread {
                                    alert("500: " + (resp.first as Message).errorDetails).show()
                                }
                            }

                            HttpHelper.ERROR_PERMISSIONS ->
                            {
                                uiThread {
                                    alert(this@VoiceActivity.getString(R.string.alert_noInternetPermission)).show()
                                }
                            }

                            HttpHelper.ERROR_NO_CONNECTION ->
                            {
                                uiThread {
                                    alert(this@VoiceActivity.getString(R.string.alert_noInternetConnection)).show()
                                }
                            }

                            else ->
                            {
                                uiThread {
                                    alert(getString(R.string.text_general_error, (resp.first as Message).errorDetails)).show()
                                }
                            }
                        }

                        uiThread {
                            saveTaskButton.isEnabled = false
                        }
                    }
                }
            }

            1234-> {
                if(resultCode == Activity.RESULT_OK)
                {
                    textStatus.text = getString(R.string.text_voice_understood)
                    val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) ?: arrayListOf("No, non ho capito in realtà")
                    textSentence.setText(matches[0])
                }
                else
                {
                    textStatus.text = getString(R.string.text_voice_error)
                }
            }

            SessionHelper.LOGIN_REQUEST_CODE->{
                if(resultCode == Activity.RESULT_OK)
                {
                    toolbar.snackbar("Sei collegato come: " + session.user.email).show()
                    textStatus.text = getString(R.string.text_voice_retry)
                }
                else
                {
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
                }
            }
        }
    }

}
