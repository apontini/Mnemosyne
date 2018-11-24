package ap.mnemosyne.activities

import android.app.Activity
import android.os.Bundle
import android.preference.Preference
import apontini.mnemosyne.R
import android.preference.PreferenceFragment
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.google.android.gms.location.places.ui.PlacePicker
import kotlinx.android.synthetic.main.activity_settings.*
import android.content.Intent
import android.util.Log
import ap.mnemosyne.enums.ParamsName
import ap.mnemosyne.http.HttpHelper
import ap.mnemosyne.parameters.ParametersHelper
import ap.mnemosyne.resources.LocationParameter
import ap.mnemosyne.resources.TimeParameter
import ap.mnemosyne.session.SessionHelper
import okhttp3.Request
import org.jetbrains.anko.*
import org.jetbrains.anko.design.snackbar
import org.joda.time.format.DateTimeFormat


class SettingsActivity : AppCompatActivity()
{

    val thisActivity = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fragmentManager
            .beginTransaction()
            .replace(R.id.settingsLayout, Preferences())
            .commit()
    }

    class Preferences : PreferenceFragment()
    {
        private lateinit var session : SessionHelper
        private lateinit var parameters : ParametersHelper
        var locationHouse : LocationParameter? = null
        var locationWork : LocationParameter? = null
        var timeLunch : TimeParameter? = null
        var timeDinner : TimeParameter? = null
        var timeBed : TimeParameter? = null
        var timeWork : TimeParameter? = null

        override fun onCreate(savedInstanceState: Bundle?)
        {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)

            session = SessionHelper(this.activity)
            parameters = ParametersHelper(this.activity)

            printParameters()

            findPreference("location_house").onPreferenceClickListener = Preference.OnPreferenceClickListener{
                if(locationHouse != null)
                {
                    alert(getString(R.string.alert_settings_parametersChange)) {
                        yesButton {
                            val builder: PlacePicker.IntentBuilder = PlacePicker.IntentBuilder()
                            startActivityForResult(builder.build(this@Preferences.activity), 0)
                        }
                        noButton { }
                    }.show()
                }
                else
                {
                    val builder: PlacePicker.IntentBuilder = PlacePicker.IntentBuilder()
                    startActivityForResult(builder.build(this@Preferences.activity), 0)
                }
                true
            }

            findPreference("location_work").onPreferenceClickListener = Preference.OnPreferenceClickListener{
                if(locationWork != null)
                {
                    alert(getString(R.string.alert_settings_parametersChange)) {
                        yesButton {
                            val builder: PlacePicker.IntentBuilder = PlacePicker.IntentBuilder()
                            startActivityForResult(builder.build(this@Preferences.activity), 1)
                        }
                        noButton { }
                    }.show()
                }
                else
                {
                    val builder: PlacePicker.IntentBuilder = PlacePicker.IntentBuilder()
                    startActivityForResult(builder.build(this@Preferences.activity), 1)
                }
                true
            }

            findPreference("time_lunch").onPreferenceClickListener = Preference.OnPreferenceClickListener{
                if(locationWork != null)
                {
                    alert(getString(R.string.alert_settings_parametersChange)) {
                        yesButton {
                            val intent = Intent(this@Preferences.activity, TimeIntervalPickerActivity::class.java)
                            startActivityForResult(intent, 2)
                        }
                        noButton { }
                    }.show()
                }
                else
                {
                    val intent = Intent(this@Preferences.activity, TimeIntervalPickerActivity::class.java)
                    startActivityForResult(intent, 2)
                }

                true
            }

            findPreference("time_dinner").onPreferenceClickListener = Preference.OnPreferenceClickListener{
                if(locationWork != null)
                {
                    alert(getString(R.string.alert_settings_parametersChange)) {
                        yesButton {
                            val intent = Intent(this@Preferences.activity, TimeIntervalPickerActivity::class.java)
                            startActivityForResult(intent, 3)
                        }
                        noButton { }
                    }.show()
                }
                else
                {
                    val intent = Intent(this@Preferences.activity, TimeIntervalPickerActivity::class.java)
                    startActivityForResult(intent, 3)
                }
                true
            }

            findPreference("time_bed").onPreferenceClickListener = Preference.OnPreferenceClickListener{
                if(locationWork != null)
                {
                    alert(getString(R.string.alert_settings_parametersChange)) {
                        yesButton {
                            val intent = Intent(this@Preferences.activity, TimeIntervalPickerActivity::class.java)
                            startActivityForResult(intent, 4)
                        }
                        noButton { }
                    }.show()
                }
                else
                {
                    val intent = Intent(this@Preferences.activity, TimeIntervalPickerActivity::class.java)
                    startActivityForResult(intent, 4)
                }
                true
            }

            findPreference("time_work").onPreferenceClickListener = Preference.OnPreferenceClickListener{
                if(locationWork != null)
                {
                    alert(getString(R.string.alert_settings_parametersChange)) {
                        yesButton {
                            val intent = Intent(this@Preferences.activity, TimeIntervalPickerActivity::class.java)
                            startActivityForResult(intent, 5)
                        }
                        noButton { }
                    }.show()
                }
                else
                {
                    val intent = Intent(this@Preferences.activity, TimeIntervalPickerActivity::class.java)
                    startActivityForResult(intent, 5)
                }
                true
            }

            findPreference("password").onPreferenceClickListener = Preference.OnPreferenceClickListener{

                alert("Non implementato") {  }.show()
                true
            }

            findPreference("log_out").onPreferenceClickListener = Preference.OnPreferenceClickListener{
                alert("Non implementato") {  }.show()
                true
            }
        }

        private fun printParameters()
        {
            parameters.updateParametersAndDo{
                locationHouse = parameters.getLocalParameter(ParamsName.location_house) as LocationParameter?
                locationWork = parameters.getLocalParameter(ParamsName.location_work) as LocationParameter?
                timeLunch = parameters.getLocalParameter(ParamsName.time_lunch) as TimeParameter?
                timeDinner = parameters.getLocalParameter(ParamsName.time_dinner) as TimeParameter?
                timeBed = parameters.getLocalParameter(ParamsName.time_bed) as TimeParameter?
                timeWork = parameters.getLocalParameter(ParamsName.time_work) as TimeParameter?

                Log.d("PARAM", locationHouse.toString())

                findPreference("location_house").summary = if(locationHouse != null) "${(locationHouse as LocationParameter).location?.lat}, ${(locationHouse as LocationParameter).location?.lon}"
                else getString(R.string.text_settings_notDefined)
                findPreference("location_work").summary = if(locationWork != null) "${(locationWork as LocationParameter).location?.lat}, ${(locationWork as LocationParameter).location?.lon}"
                else getString(R.string.text_settings_notDefined)

                findPreference("time_lunch").summary = if(timeLunch != null ) "${(timeLunch as TimeParameter).fromTime.toString(
                    DateTimeFormat.forPattern("HH:mm"))} - ${(timeLunch as TimeParameter).toTime.toString(
                    DateTimeFormat.forPattern("HH:mm"))}"  else getString(R.string.text_settings_notDefined)

                findPreference("time_dinner").summary = if(timeDinner != null ) "${(timeDinner as TimeParameter).fromTime.toString(
                    DateTimeFormat.forPattern("HH:mm"))} - ${(timeDinner as TimeParameter).toTime.toString(
                    DateTimeFormat.forPattern("HH:mm"))}"  else getString(R.string.text_settings_notDefined)

                findPreference("time_bed").summary = if(timeBed != null ) "${(timeBed as TimeParameter).fromTime.toString(
                    DateTimeFormat.forPattern("HH:mm"))} - ${(timeBed as TimeParameter).toTime.toString(
                    DateTimeFormat.forPattern("HH:mm"))}"  else getString(R.string.text_settings_notDefined)

                findPreference("time_work").summary = if(timeWork != null ) "${(timeWork as TimeParameter).fromTime.toString(
                    DateTimeFormat.forPattern("HH:mm"))} - ${(timeWork as TimeParameter).toTime.toString(
                    DateTimeFormat.forPattern("HH:mm"))}"  else getString(R.string.text_settings_notDefined)
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
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
                        val place = PlacePicker.getPlace(this.activity, data)
                        //val param = LocationParameter(paramName, session.user.email, )
                    }

                    2,3,4,5 ->
                    {

                    }
                }

                //controllare se inviare POST o PUT
            }
            else if(paramName == null)
            {
                when (requestCode)
                {
                    100 ->
                    {
                        if(resultCode == Activity.RESULT_OK)
                        {
                            snackbar(this.activity.toolbar, "Sei collegato come: " + session.user.email).show()
                            printParameters()
                        }
                    }
                }
            }
        }
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
}
