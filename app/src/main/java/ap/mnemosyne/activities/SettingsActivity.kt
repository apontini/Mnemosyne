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
import ap.mnemosyne.enums.ParamsName
import ap.mnemosyne.http.HttpHelper
import ap.mnemosyne.resources.LocationParameter
import ap.mnemosyne.resources.Parameter
import ap.mnemosyne.session.SessionHelper
import okhttp3.Request
import org.jetbrains.anko.*
import java.io.IOException
import java.nio.charset.StandardCharsets
import org.joda.time.format.DateTimeFormat


class SettingsActivity : AppCompatActivity()
{

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

        override fun onCreate(savedInstanceState: Bundle?)
        {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)

            session = SessionHelper(this.activity)

            val fmt = DateTimeFormat.forPattern("HH:mm")

            val locationHouse = try { Parameter.fromJSON(defaultSharedPreferences.getString("location_house", "").byteInputStream(StandardCharsets.UTF_8)) as LocationParameter}
                catch(ioe : IOException) { null }
            val locationWork = try { Parameter.fromJSON(defaultSharedPreferences.getString("location_work", "").byteInputStream(StandardCharsets.UTF_8)) as LocationParameter}
                catch(ioe : IOException) { null }
            val timeLunch = defaultSharedPreferences.getString("time_lunch", "")
            val timeDinner = defaultSharedPreferences.getString("time_dinner", "")
            val timeBed = defaultSharedPreferences.getString("time_bed", "")
            val timeWork = defaultSharedPreferences.getString("time_work", "")

            findPreference("location_house").summary = if(locationHouse != null) "${locationHouse.location?.lat}, ${locationHouse.location?.lon}"
                                                            else getString(R.string.text_settings_notDefined)
            findPreference("location_work").summary = if(locationWork != null) "${locationWork.location?.lat}, ${locationWork.location?.lon}"
                                                            else getString(R.string.text_settings_notDefined)
            findPreference("time_lunch").summary = if(timeLunch=="") getString(R.string.text_settings_notDefined) else timeLunch
            findPreference("time_dinner").summary = if(timeDinner=="") getString(R.string.text_settings_notDefined) else timeDinner
            findPreference("time_bed").summary = if(timeBed=="") getString(R.string.text_settings_notDefined) else timeBed
            findPreference("time_work").summary = if(timeWork=="") getString(R.string.text_settings_notDefined) else timeWork

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
