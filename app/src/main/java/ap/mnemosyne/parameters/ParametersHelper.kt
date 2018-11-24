package ap.mnemosyne.parameters

import android.app.Activity
import android.content.Intent
import android.util.Log
import ap.mnemosyne.activities.LoginActivity
import ap.mnemosyne.enums.ParamsName
import ap.mnemosyne.http.HttpHelper
import ap.mnemosyne.resources.Parameter
import ap.mnemosyne.resources.ResourceList
import ap.mnemosyne.session.SessionHelper
import apontini.mnemosyne.R
import okhttp3.Request
import org.jetbrains.anko.alert
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.joda.time.LocalDateTime
import org.joda.time.Minutes
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.nio.charset.StandardCharsets

class ParametersHelper(act: Activity)
{

    companion object
    {
        val dateTimeFormat : DateTimeFormatter by lazy { return@lazy DateTimeFormat.forPattern("yyyy-MM-dd HH:mm") }
        private const val MIN_UPDATE = 1
        const val LAST_REFRESH : String = "last_refresh"
    }

    val act = act
    val session = SessionHelper(act)

    fun updateParametersAndDo(forceUpdate : Boolean = false, doWhat: () -> (Unit))
    {
        val refreshed = LocalDateTime.parse(act.defaultSharedPreferences.getString(ParametersHelper.LAST_REFRESH,
            "1970-01-01 00:00"), ParametersHelper.dateTimeFormat)
        val now = LocalDateTime.now()
        if (forceUpdate || Minutes.minutesBetween(refreshed, now).minutes >= MIN_UPDATE)
        {
            Log.d("PARAMETERS", "Requesting Parameter Update")
            doAsync {
                val request = Request.Builder()
                    .addHeader("Cookie", "JSESSIONID=" + session.user
                        .sessionID)
                    .url(HttpHelper.REST_PARAMETER_URL)
                    .build()
                val resp = HttpHelper(act).request(request, true)
                var error = false
                when (resp.second.code())
                {
                    401 ->
                    {
                        error = true
                        Log.d("SESSION", "Sessione scaduta")
                        val intent = Intent(act, LoginActivity::class.java)
                        act.startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
                    }

                    200 ->
                    {
                        (resp.first as ResourceList<Parameter>).list.forEach {
                            with(act.defaultSharedPreferences.edit()) {
                                putString(it.name.name, it.toJSON())
                                apply()
                            }
                        }

                        with(act.defaultSharedPreferences.edit()) {
                            putString(LAST_REFRESH, now.toString(dateTimeFormat))
                            apply()
                        }
                    }

                    999 ->
                    {
                        error = true
                        act.alert(act.getString(R.string.alert_noInternetPermission)) { }.show()
                    }

                    else ->
                    {
                        error = true
                        act.alert("Non ho potuto aggiornare i parametri, codice: " + resp.second.code()) { }.show()
                    }
                }
                if (!error)
                {
                    uiThread {
                        doWhat()
                    }
                }
            }
        }
        else
        {
            Log.d("PARAMETERS", "No need to update")
            doWhat()
        }
    }

    fun updateParameterAndDo(parameter : ParamsName,forceUpdate : Boolean = false, doWhat: () -> (Unit))
    {
        val refreshed = LocalDateTime.parse(act.defaultSharedPreferences.getString(ParametersHelper.LAST_REFRESH,
            "1970-01-01 00:00"), ParametersHelper.dateTimeFormat)
        val now = LocalDateTime.now()
        if (forceUpdate || Minutes.minutesBetween(refreshed, now).minutes >= MIN_UPDATE)
        {
            doAsync {
                val request = Request.Builder()
                    .addHeader("Cookie", "JSESSIONID=" + session.user
                        .sessionID)
                    .url(HttpHelper.REST_PARAMETER_URL + "/" + parameter.name)
                    .build()
                val resp = HttpHelper(act).request(request, true)
                var error = false
                when (resp.second.code())
                {
                    401 ->
                    {
                        error = true
                        Log.d("SESSION", "Sessione scaduta")
                        val intent = Intent(act, LoginActivity::class.java)
                        act.startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
                    }

                    200 ->
                    {
                        val param = resp.first as Parameter
                        with(act.defaultSharedPreferences.edit()) {
                            putString(param.name.name, param.toJSON())
                            apply()
                        }
                    }

                    999 ->
                    {
                        error = true
                        act.alert(act.getString(R.string.alert_noInternetPermission)) { }.show()
                    }

                    else ->
                    {
                        error = true
                        act.alert("Non ho potuto aggiornare i parametri, codice: " + resp.second.code()) { }.show()
                    }
                }
                if (!error)
                {
                    uiThread {
                        doWhat()
                    }
                }
            }
        }
        else
        {
            doWhat()
        }
    }

    fun resetLocalParameters()
    {
        ParamsName.values().forEach {
            with(act.defaultSharedPreferences.edit()){
                remove(it.name)
                putString(LAST_REFRESH, "1970-01-01 00:00")
                apply()
            }
        }
    }

    fun getLocalParameter(p : ParamsName) : Parameter?
    {
        return try
        {
            Parameter.fromJSON(act.defaultSharedPreferences.getString(p.name, "").byteInputStream(
            StandardCharsets.UTF_8))
        }
        catch (e: Exception)
        {
            e.printStackTrace()
            null
        }

    }
}