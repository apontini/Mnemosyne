package ap.mnemosyne.activities

import android.app.Activity
import android.os.Bundle
import android.preference.Preference
import ap.mnemosyne.R
import android.preference.PreferenceFragment
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import com.google.android.gms.location.places.ui.PlacePicker
import kotlinx.android.synthetic.main.activity_settings.*
import android.content.Intent
import android.location.Address
import android.util.Log
import ap.mnemosyne.enums.ParamsName
import ap.mnemosyne.http.HttpHelper
import ap.mnemosyne.parameters.ParametersHelper
import ap.mnemosyne.resources.*
import ap.mnemosyne.session.SessionHelper
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.jetbrains.anko.*
import org.jetbrains.anko.design.snackbar
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat
import java.nio.charset.StandardCharsets
import android.location.Geocoder
import org.jetbrains.anko.design.textInputEditText
import java.util.*


class SettingsActivity : AppCompatActivity()
{

    companion object
    {
        const val LOGOUT_RESULT_CODE = 100
    }

    val thisActivity = this
    private lateinit var session : SessionHelper
    private lateinit var fragment : Preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        session = SessionHelper(this)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fragment = Preferences()

        fragmentManager
            .beginTransaction()
            .replace(R.id.settingsLayout, fragment)
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

            Log.d("LIFECYCLE", "onCreate")

            session = SessionHelper(this.activity)
            parameters = ParametersHelper(this.activity)

            session.checkSessionValidity{ printParameters() }

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
                if(timeLunch != null)
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
                if(timeDinner != null)
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
                if(timeBed != null)
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
                if(timeWork != null)
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

                val intent = Intent(this@Preferences.activity, PasswordChangeActivity::class.java)
                startActivityForResult(intent, PasswordChangeActivity.PSW_RESULT_CODE)
                true
            }

            findPreference("log_out").onPreferenceClickListener = Preference.OnPreferenceClickListener{
                alert(getString(R.string.alert_settings_confirm)) {
                    yesButton {
                        doLogout()
                    }
                    noButton {}
                }.show()
                true
            }
        }

        private fun doLogout()
        {
            val request = Request.Builder()
                .addHeader("Cookie" , "JSESSIONID=${session.user.sessionID}")
                .url(HttpHelper.AUTH_URL)
                .delete()
                .build()

            doAsync {
                val resp = HttpHelper(act).request(request, true)
                var error = false
                when(resp.second.code())
                {
                    401 -> {
                        Log.d("SESSION", "Sessione scaduta")
                    }

                    200 -> {
                        Log.d("SESSION", "Sessione Eliminata")
                    }

                    HttpHelper.ERROR_PERMISSIONS ->{
                        uiThread { this@Preferences.activity.alert(this@Preferences.activity.getString(R.string.alert_noInternetPermission)) {  }.show() }
                        error = true
                    }

                    HttpHelper.ERROR_NO_CONNECTION ->{
                        uiThread { this@Preferences.activity.alert(this@Preferences.activity.getString(R.string.alert_noInternetConnection)) {  }.show() }
                        error = true
                    }

                    else ->{
                        uiThread { this@Preferences.activity.alert("${resp.second.code()}") {  }.show() }
                        error = true
                    }
                }
                if(!error)
                {
                    this@Preferences.activity.setResult(LOGOUT_RESULT_CODE)
                    this@Preferences.activity.finish()
                }
            }
        }

        fun printParameters()
        {
            parameters.updateParametersAndDo{
                locationHouse = parameters.getLocalParameter(ParamsName.location_house) as LocationParameter?
                locationWork = parameters.getLocalParameter(ParamsName.location_work) as LocationParameter?
                timeLunch = parameters.getLocalParameter(ParamsName.time_lunch) as TimeParameter?
                timeDinner = parameters.getLocalParameter(ParamsName.time_dinner) as TimeParameter?
                timeBed = parameters.getLocalParameter(ParamsName.time_bed) as TimeParameter?
                timeWork = parameters.getLocalParameter(ParamsName.time_work) as TimeParameter?

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

                val gcd = Geocoder(context, Locale.getDefault())
                lateinit var addresses : MutableList<Address>
                if(locationHouse != null)
                {
                    addresses = gcd.getFromLocation((locationHouse as LocationParameter).location?.lat!!, (locationHouse as LocationParameter).location?.lon!!, 1)
                    if (addresses.size > 0)
                    {
                        if(addresses[0].thoroughfare != null)
                        {
                            findPreference("location_house").summary = addresses[0].thoroughfare
                        }
                    }
                }

                if(locationWork != null)
                {
                    addresses = gcd.getFromLocation((locationWork as LocationParameter).location?.lat!!, (locationWork as LocationParameter).location?.lon!!, 1)
                    if (addresses.size > 0)
                    {
                        if(addresses[0].thoroughfare != null)
                        {
                            findPreference("location_work").summary = addresses[0].thoroughfare
                        }
                    }
                }
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
                        val def = defaultSharedPreferences.getString(paramName.name, "")
                        if(def == "")
                        {
                            Log.d("PARAMETER", "Creating parameter")
                            val param = LocationParameter(paramName, session.user.email, Point(place.latLng.latitude, place.latLng.longitude), -1, null)
                            bRequest.post(RequestBody.create(MediaType.parse("application/json"), param.toJSON()))
                        }
                        else
                        {
                            Log.d("PARAMETER", "Updating parameter")
                            val pre = Parameter.fromJSON(def.byteInputStream(StandardCharsets.UTF_8)) as LocationParameter
                            val param = LocationParameter(paramName, session.user.email, Point(place.latLng.latitude, place.latLng.longitude), pre.cellID, pre.ssid)
                            bRequest.put(RequestBody.create(MediaType.parse("application/json"), param.toJSON()))
                        }
                    }

                    2,3,4,5 ->
                    {
                        val fromTime = data?.getSerializableExtra("fromTime") as LocalTime
                        val toTime = data?.getSerializableExtra("toTime") as LocalTime
                        val def = defaultSharedPreferences.getString(paramName.name, "")
                        if(def == "")
                        {
                            Log.d("PARAMETER", "Creating parameter")
                            val param = TimeParameter(paramName, session.user.email, fromTime, toTime)
                            bRequest.post(RequestBody.create(MediaType.parse("application/json"), param.toJSON()))
                        }
                        else
                        {
                            Log.d("PARAMETER", "Updating parameter")
                            val param = TimeParameter(paramName, session.user.email, fromTime, toTime)
                            bRequest.put(RequestBody.create(MediaType.parse("application/json"), param.toJSON()))
                        }
                    }
                }

                doAsync {
                    val resp = HttpHelper(this@Preferences.activity).request(bRequest.build(), true)
                    when(resp.second.code())
                    {
                        201, 200 ->
                        {
                            this@Preferences.activity.toolbar.snackbar(getString(R.string.text_settings_successUpdate)).show()
                            when (requestCode)
                            {
                                0, 1 ->
                                {
                                    uiThread {
                                        findPreference(paramName.name).summary = "${(resp.first as LocationParameter).location.lat}, ${(resp.first as LocationParameter).location.lon}"
                                    }
                                    with(defaultSharedPreferences.edit())
                                    {
                                        putString(paramName.name, (resp.first as LocationParameter).toJSON())
                                        apply()
                                    }
                                }

                                2,3,4,5 ->
                                {
                                    uiThread {
                                        findPreference(paramName.name).summary = "${(resp.first as TimeParameter).fromTime.toString(DateTimeFormat.forPattern("HH:mm"))}, " +
                                            (resp.first as TimeParameter).toTime.toString(DateTimeFormat.forPattern("HH:mm"))
                                    }
                                    with(defaultSharedPreferences.edit())
                                    {
                                        putString(paramName.name, (resp.first as TimeParameter).toJSON())
                                        apply()
                                    }
                                }
                            }
                        }

                        401 ->
                        {
                            Log.d("SESSION", "Sessione scaduta")
                            val intent = Intent(this@Preferences.activity, LoginActivity::class.java)
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
                                alert(this@Preferences.activity.getString(R.string.alert_noInternetPermission)).show()
                            }
                        }

                        HttpHelper.ERROR_NO_CONNECTION ->
                        {
                            uiThread {
                                alert(this@Preferences.activity.getString(R.string.alert_noInternetConnection)).show()
                            }
                        }

                        HttpHelper.ERROR_UNKNOWN ->
                        {
                            uiThread {
                                alert(this@Preferences.activity.getString(R.string.alert_generalError)).show()
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
            else if(paramName == null)
            {
                when (requestCode)
                {
                    SessionHelper.LOGIN_REQUEST_CODE ->
                    {
                        if(resultCode == Activity.RESULT_OK)
                        {
                            this.activity.toolbar.snackbar(getString(R.string.login_connectedAs, session.user.email)).show()
                            printParameters()
                        }
                    }

                    PasswordChangeActivity.PSW_RESULT_CODE ->
                    {
                        if(resultCode == Activity.RESULT_OK)
                        {
                            this.activity.toolbar.snackbar(getString(R.string.info_passwordChanged)).show()
                        }
                    }
                }
            }
        }
    }

    override fun onRestart()
    {
        super.onRestart()
        session.checkSessionValidity {
            fragment.printParameters()
            Unit
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode)
        {
            SessionHelper.LOGIN_REQUEST_CODE ->
            {
                if(resultCode == Activity.RESULT_OK)
                {
                    val splashIntent = Intent(this, SplashActivity::class.java)
                    splashIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(splashIntent)
                }
            }
        }
    }
}
