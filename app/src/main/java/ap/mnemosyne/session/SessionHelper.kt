package ap.mnemosyne.session

import android.content.Context
import android.content.SharedPreferences
import ap.mnemosyne.resources.User
import apontini.mnemosyne.R
import org.jetbrains.anko.toast

class SessionHelper(ctx : Context)
{
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
}