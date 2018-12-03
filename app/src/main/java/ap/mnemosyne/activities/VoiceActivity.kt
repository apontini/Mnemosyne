package ap.mnemosyne.activities

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity;
import apontini.mnemosyne.R

import kotlinx.android.synthetic.main.activity_voice.*
import android.speech.RecognizerIntent
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import android.view.View
import ap.mnemosyne.enums.ParamsName
import ap.mnemosyne.http.HttpHelper
import ap.mnemosyne.permissions.PermissionsHelper
import ap.mnemosyne.resources.*
import ap.mnemosyne.session.SessionHelper
import com.google.android.gms.location.places.ui.PlacePicker
import kotlinx.android.synthetic.main.content_voice.*
import okhttp3.*
import org.jetbrains.anko.*
import org.jetbrains.anko.design.snackbar
import org.joda.time.LocalTime
import java.util.*

class VoiceActivity : AppCompatActivity()
{
    private lateinit var session : SessionHelper

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        session = SessionHelper(this)
        setContentView(R.layout.activity_voice)
        setSupportActionBar(toolbar)

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
    }

    private fun sendTask(sessionid : String)
    {

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        {
            alert("I servizi di localizzazione non sono abilitati!").show()
            return
        }

        progressBar.visibility = View.VISIBLE
        textStatus.text = getString(R.string.text_voice_waitPosition)
        textSentence.visibility = View.INVISIBLE
        saveTaskButton.visibility = View.INVISIBLE

        // Define a listener that responds to location updates
        val locationListener = object : LocationListener
        {

            override fun onLocationChanged(location: Location)
            {
                locationManager.removeUpdates(this)
                textStatus.text = getString(R.string.text_voice_waitServer)
                val body = FormBody.Builder().add("sentence", textSentence.text.toString()).add("lat", location.latitude.toString())
                    .add("lon", location.longitude.toString()).build()

                val request = Request.Builder()
                    .addHeader("Cookie" , "JSESSIONID=" + sessionid)
                    .url(HttpHelper.PARSE_URL)
                    .post(body)
                    .build()

                doAsync {
                    val response : Pair<Resource?, Response> = HttpHelper(this@VoiceActivity).request(request, true)
                    when(response.second.code())
                    {
                        200->{
                            val returnIntent = Intent()
                            returnIntent.putExtra("resultTask", response.first as Task)
                            setResult(Activity.RESULT_OK, returnIntent)
                            finish()
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
                                            yesButton { askParameter(ParamsName.valueOf(respMessage.errorDetails)) }
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
                                        textStatus.text = "SQLException: " + respMessage.errorDetails
                                    }

                                    "PRSR09" -> {
                                        textStatus.text = "ServletException: " + respMessage.errorDetails
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
                            textStatus.text =  getString(R.string.text_general_error, getString(R.string.alert_noInternetPermission))
                        }

                        else ->
                        {
                            val respMessage = response.first as Message
                            textStatus.text =  getString(R.string.text_general_error, respMessage.errorCode + ": " + respMessage.errorDetails)
                        }
                    }

                    uiThread {
                        progressBar.visibility = View.INVISIBLE
                        textSentence.visibility = View.VISIBLE
                        saveTaskButton.visibility = View.VISIBLE
                    }
                }

            }

            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

            override fun onProviderEnabled(provider: String) {}

            override fun onProviderDisabled(provider: String) {}
        }

        if(!PermissionsHelper.checkPositionPermission(this))
        {
            alert(getString(R.string.alert_noPositionPermission)){
                okButton { finish() }
            }.show()
            return
        }

        if(!PermissionsHelper.checkCoarsePositionPermission(this))
        {
            alert(getString(R.string.alert_noCoarsePositionPermission)){
                okButton { finish() }
            }.show()
            return
        }


        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
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

    private fun startVoiceRecognitionActivity()
    {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
            "Speech recognition demo")
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
                                snackbar(toolbar, getString(R.string.text_settings_successUpdate)).show()
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

                            else ->
                            {
                                uiThread {
                                    alert(getString(R.string.text_general_error, (resp.first as Message).errorDetails)).show()
                                }
                            }
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
