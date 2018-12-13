package ap.mnemosyne.session

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import ap.mnemosyne.activities.LoginActivity
import ap.mnemosyne.http.HttpHelper
import ap.mnemosyne.resources.User
import ap.mnemosyne.R
import okhttp3.Request
import org.jetbrains.anko.alert
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread

class SessionHelper(ctx : Context)
{
    companion object
    {
        const val LOGIN_REQUEST_CODE = 100
    }


    val thisCtx = ctx
    val sharedPref : SharedPreferences =
            ctx.getSharedPreferences(ctx.getString(R.string.sharedPreferences_user_FILE), Context.MODE_PRIVATE)
    var user : User
        get()
        {
            val uemail = sharedPref.getString(thisCtx.getString(R.string.sharedPreferences_user_mail),"")
            val usession = sharedPref.getString(thisCtx.getString(R.string.sharedPreferences_user_sessionid), "")
            return User(usession, uemail)
        }
        set(u)
        {
            with(sharedPref.edit())
            {
                putString(thisCtx.getString(R.string.sharedPreferences_user_sessionid), u.sessionID)
                putString(thisCtx.getString(R.string.sharedPreferences_user_mail), u.email)
                if(!commit()) thisCtx.toast("Errore nel salvare i dati, riavvia l'applicazione").show()
            }
        }

    fun checkSessionValidity(onSuccess: () -> (Unit) ? = {})
    {
        val sessionid = user.sessionID
        val useremail = user.email

        if(sessionid == "" || useremail == "")
        {
            Log.d("SESSION", "Preferences non trovate")
            val intent = Intent(thisCtx, LoginActivity::class.java)
            if(thisCtx is Activity) thisCtx.startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
        }
        else
        {
            val request = Request.Builder()
                .addHeader("Cookie" , "JSESSIONID=$sessionid")
                .url(HttpHelper.REST_USER_URL)
                .build()
            var error = false
            doAsync {
                val resp = HttpHelper(thisCtx).request(request, true)
                when(resp.second.code())
                {
                    401 -> {
                        Log.d("SESSION", "Sessione scaduta")
                        val intent = Intent(thisCtx, LoginActivity::class.java)
                        if(thisCtx is Activity) thisCtx.startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
                        error = true
                    }

                    200 -> {
                        Log.d("SESSION", "Sessione valida")
                    }

                    HttpHelper.ERROR_PERMISSIONS ->{
                        uiThread { thisCtx.alert(thisCtx.getString(R.string.alert_noInternetPermission)) {  }.show() }
                        error = true
                    }

                    HttpHelper.ERROR_NO_CONNECTION ->{
                        uiThread { thisCtx.alert(thisCtx.getString(R.string.alert_noInternetConnection)) {  }.show() }
                        error = true
                    }

                    else ->{
                        uiThread { thisCtx.alert("${resp.second.code()}") {  }.show() }
                    }
                }
                uiThread {
                    if(!error)
                    {
                        onSuccess()
                    }
                }
            }
        }
    }
}