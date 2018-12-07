package ap.mnemosyne.parameters

import android.app.Activity
import android.content.Intent
import android.util.Log
import ap.mnemosyne.activities.LoginActivity
import ap.mnemosyne.enums.ParamsName
import ap.mnemosyne.http.HttpHelper
import ap.mnemosyne.session.SessionHelper
import ap.mnemosyne.R
import ap.mnemosyne.resources.Parameter
import ap.mnemosyne.resources.ResourceList
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
import java.util.concurrent.locks.ReentrantLock

class ParametersHelper(val act: Activity)
{

    companion object
    {
        val dateTimeFormat : DateTimeFormatter by lazy { return@lazy DateTimeFormat.forPattern("yyyy-MM-dd HH:mm") }
        private const val MIN_UPDATE = 3
        const val LAST_REFRESH : String = "last_refresh"
        val lock by lazy { ReentrantLock() }
    }

    val session = SessionHelper(act)

    fun updateParametersAndDo(forceUpdate : Boolean = false, doWhatError: (Int, String) -> Unit = {_,p1 -> act.alert(p1).show()}, doWhat: () -> (Unit))
    {
        lock.lock()
        val refreshed = LocalDateTime.parse(act.defaultSharedPreferences.getString(ParametersHelper.LAST_REFRESH,
            "1970-01-01 00:00"), ParametersHelper.dateTimeFormat)
        val now = LocalDateTime.now()
        Log.d("LOCK", refreshed.toString())
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
                var error = 0
                var errorString = ""
                when (resp.second.code())
                {
                    401 ->
                    {
                        error = 401
                        Log.d("SESSION", "Sessione scaduta")
                        val intent = Intent(act, LoginActivity::class.java)
                        act.startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
                    }

                    200 ->
                    {
                        val map = mutableMapOf<ParamsName, Boolean>()
                        with(map)
                        {
                            ParamsName.values().forEach {
                                put(it,false)
                            }
                        }

                        (resp.first as ResourceList<Parameter>).list.forEach {
                            with(act.defaultSharedPreferences.edit()) {
                                putString(it.name.name, it.toJSON())
                                map[it.name] = true
                                apply()
                            }
                        }

                        map.filter { !it.value }.forEach {
                            with(act.defaultSharedPreferences.edit()) {
                                putString(it.key.name, "")
                                apply()
                            }
                        }

                        with(act.defaultSharedPreferences.edit()) {
                            putString(LAST_REFRESH, now.toString(dateTimeFormat))
                            apply()
                        }
                    }

                    HttpHelper.ERROR_PERMISSIONS ->
                    {
                        error = HttpHelper.ERROR_PERMISSIONS
                        errorString = act.getString(R.string.alert_noInternetPermission)

                    }

                    HttpHelper.ERROR_NO_CONNECTION ->
                    {
                        error = HttpHelper.ERROR_PERMISSIONS
                        errorString = act.getString(R.string.alert_noInternetConnection)

                    }

                    else ->
                    {
                        error = -1
                        uiThread {act.alert("Non ho potuto aggiornare i parametri, codice: " + resp.second.code()) { }.show()  }
                    }
                }
                if (error == 0)
                {
                    uiThread {
                        doWhat()
                    }
                }
                else if(error>0)
                {
                    uiThread {
                        doWhatError(error, errorString)
                    }
                }
            }
        }
        else
        {
            Log.d("PARAMETERS", "No need to update")
            doWhat()
        }
        lock.unlock()
    }

    fun updateParameterAndDo(parameter : ParamsName,forceUpdate : Boolean = false, doWhatError: (Int, String) -> Unit = {_,p1 -> act.alert(p1).show()}, doWhat: () -> (Unit))
    {
        lock.lock()
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
                var error = 0
                var errorString = ""
                when (resp.second.code())
                {
                    401 ->
                    {
                        error = 401
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

                    HttpHelper.ERROR_PERMISSIONS ->
                    {
                        error = HttpHelper.ERROR_PERMISSIONS
                        uiThread { act.alert(act.getString(R.string.alert_noInternetPermission)) { }.show() }

                    }

                    HttpHelper.ERROR_NO_CONNECTION ->
                    {
                        error = HttpHelper.ERROR_PERMISSIONS
                        uiThread { act.alert(act.getString(R.string.alert_noInternetConnection)) { }.show() }

                    }

                    else ->
                    {
                        error = -1
                        uiThread { act.alert("Non ho potuto aggiornare i parametri, codice: " + resp.second.code()) { }.show() }

                    }
                }
                if (error == 0)
                {
                    uiThread {
                        doWhat()
                    }
                }
                else if(error>0)
                {
                    uiThread {
                        doWhatError(error,errorString)
                    }
                }
            }
        }
        else
        {
            doWhat()
        }
        lock.unlock()
    }

    fun resetLocalParameters()
    {
        lock.lock()
        ParamsName.values().forEach {
            with(act.defaultSharedPreferences.edit()){
                remove(it.name)
                putString(LAST_REFRESH, "1970-01-01 00:00")
                apply()
            }
        }
        lock.unlock()
    }

    fun getLocalParameter(p : ParamsName) : Parameter?
    {
        lock.lock()
        return try
        {
            Parameter.fromJSON(act.defaultSharedPreferences.getString(p.name, "").byteInputStream(
            StandardCharsets.UTF_8))
        }
        catch (e: Exception)
        {
            null
        }
        finally
        {
            lock.unlock()
        }

    }
}