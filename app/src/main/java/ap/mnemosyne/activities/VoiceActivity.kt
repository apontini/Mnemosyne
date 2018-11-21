package ap.mnemosyne.activities

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity;
import apontini.mnemosyne.R

import kotlinx.android.synthetic.main.activity_voice.*
import android.speech.RecognizerIntent
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.text.Editable
import android.util.Log
import android.view.View
import ap.mnemosyne.httphandler.HttpHandler
import ap.mnemosyne.permissions.PermissionsHandler
import ap.mnemosyne.resources.Message
import ap.mnemosyne.resources.Resource
import ap.mnemosyne.resources.Task
import ap.mnemosyne.session.SessionManager
import kotlinx.android.synthetic.main.content_voice.*
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
import org.jetbrains.anko.alert
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.okButton
import org.jetbrains.anko.uiThread
import java.util.*

class VoiceActivity : AppCompatActivity()
{
    private lateinit var session : SessionManager

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        session = SessionManager(this)
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

        if(!PermissionsHandler.checkMicrophonePermission(this))
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
        // Acquire a reference to the system Location Manager
        progressBar.visibility = View.VISIBLE
        textStatus.text = getString(R.string.text_voice_waitPosition)
        textSentence.visibility = View.INVISIBLE
        saveTaskButton.visibility = View.INVISIBLE

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

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
                    .url(HttpHandler.PARSE_URL)
                    .post(body)
                    .build()

                doAsync {
                    val response : Pair<Resource?, Response> = HttpHandler(this@VoiceActivity).request(request, true)
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
                            startActivityForResult(intent, 0)
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
                                        textStatus.text = getString(R.string.text_general_error, respMessage.errorDetails)
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
                                        textStatus.text = getString(R.string.text_voice_PRSR11, respMessage.errorDetails)
                                    }

                                    else -> {
                                        textStatus.text = getString(R.string.text_general_error, respMessage.errorDetails)
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
                                        textStatus.text = getString(R.string.text_general_error, respMessage.errorDetails)
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
                                        textStatus.text = getString(R.string.text_general_error, respMessage.errorDetails)
                                    }
                                }

                            }
                        }

                        999->{
                            textStatus.text = "ERRORE: " + getString(R.string.alert_noInternetPermission)
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

        if(!PermissionsHandler.checkPositionPermission(this))
        {
            alert(getString(R.string.alert_noPositionPermission)){
                okButton { finish() }
            }.show()
            return
        }

        if(!PermissionsHandler.checkCoarsePositionPermission(this))
        {
            alert(getString(R.string.alert_noCoarsePositionPermission)){
                okButton { finish() }
            }.show()
            return
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f,locationListener)
    }

    fun startVoiceRecognitionActivity()
    {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
            "Speech recognition demo")
        startActivityForResult(intent, 1234)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode)
        {
            1234-> {
                if(resultCode == Activity.RESULT_OK)
                {
                    textStatus.text = "OK"
                    // Fill the list view with the strings the recognizer thought it
                    // could have heard
                    val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) ?: arrayListOf("No, non ho capito in realtà")
                    textSentence.setText(matches[0])
                    // matches is the result of voice input. It is a list of what the
                    // user possibly said.
                    // Using an if statement for the keyword you want to use allows the
                    // use of any activity if keywords match
                    // it is possible to set up multiple keywords to use the same
                    // activity so more than one word will allow the user
                    // to use the activity (makes it so the user doesn't have to
                    // memorize words from a list)
                    // to use an activity from the voice input information simply use
                    // the following format;
                    // if (matches.contains("keyword here") { startActivity(new
                    // Intent("name.of.manifest.ACTIVITY")

                    if (matches?.contains("information") ?: false)
                    {
                        startActivity(Intent("android.intent.action.INFOSCREEN"))
                    }
                }
                else
                {
                    textStatus.text = getString(R.string.text_voice_error)
                }
            }

            0->{
                if(resultCode == Activity.RESULT_OK)
                {
                    snackbar(findViewById(R.id.layout_main), "Sei collegato come: " + session.user.email).show()
                    textStatus.text = getString(R.string.text_voice_retry)
                }
                else
                {
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivityForResult(intent, 0)
                }
            }
        }
    }

}
