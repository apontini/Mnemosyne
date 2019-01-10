package ap.mnemosyne.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import ap.mnemosyne.R
import ap.mnemosyne.http.HttpHelper
import ap.mnemosyne.session.SessionHelper
import kotlinx.android.synthetic.main.activity_password_change.*
import kotlinx.android.synthetic.main.content_password_change.*
import okhttp3.FormBody
import okhttp3.Request
import org.jetbrains.anko.*
import org.jetbrains.anko.design.snackbar

class PasswordChangeActivity : AppCompatActivity()
{
    companion object
    {
        const val PSW_RESULT_CODE = 50
    }

    lateinit var session : SessionHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionHelper(this)
        setContentView(R.layout.activity_password_change)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        changePswButton.setOnClickListener {
            pswProgress.visibility = View.VISIBLE
            oldpsw.isEnabled = false
            newpsw1.isEnabled = false
            newpsw2.isEnabled = false
            changePswButton.isEnabled = false
            cancelPswButton.isEnabled = false
            changePassword()
        }

        cancelPswButton.setOnClickListener { finish() }

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

    private fun changePassword()
    {
        val oldpswText = oldpsw.text.toString()
        val newpsw1Text = newpsw1.text.toString()
        val newpsw2Text = newpsw2.text.toString()

        Log.d("PSWS", "$oldpswText, $newpsw1Text, $newpsw2Text")

        if(newpsw1Text != newpsw2Text)
        {
            alert("Le password non corrispondono"){ title = "Errore"; okButton { } }.show()
            pswProgress.visibility = View.INVISIBLE
            oldpsw.isEnabled = true
            newpsw1.isEnabled = true
            newpsw2.isEnabled = true
            changePswButton.isEnabled = true
            cancelPswButton.isEnabled = true
            return
        }
        val body = FormBody.Builder().add("oldpsw", oldpswText).add("newpsw", newpsw1Text).build()
        val request = Request.Builder()
            .addHeader("Cookie" , "JSESSIONID=${session.user.sessionID}")
            .url(HttpHelper.REST_USER_PASSWORD_URL)
            .put(body)
            .build()

        doAsync {
            val resp = HttpHelper(this@PasswordChangeActivity).request(request, true)
            var error = false
            when(resp.second.code())
            {
                401 -> {
                    Log.d("SESSION", "Sessione scaduta")
                    error = true
                    val intent = Intent(this@PasswordChangeActivity, LoginActivity::class.java)
                    startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
                }

                200 -> {
                    Log.d("PASSWORD_CHANGE", "Password cambiata con successo")
                }

                HttpHelper.ERROR_PERMISSIONS ->{
                    uiThread { this@PasswordChangeActivity.alert(this@PasswordChangeActivity.getString(R.string.alert_noInternetPermission)) {  }.show() }
                    error = true
                }

                HttpHelper.ERROR_NO_CONNECTION ->{
                    uiThread { this@PasswordChangeActivity.alert(this@PasswordChangeActivity.getString(R.string.alert_noInternetConnection)) {  }.show() }
                    error = true
                }

                else ->{
                    uiThread { this@PasswordChangeActivity.alert("${resp.second.code()}") {  }.show() }
                    error = true
                }
            }
            if(!error)
            {
                setResult(Activity.RESULT_OK)
                finish()
            }
            uiThread {
                pswProgress.visibility = View.INVISIBLE
                oldpsw.isEnabled = true
                newpsw1.isEnabled = true
                newpsw2.isEnabled = true
                changePswButton.isEnabled = true
                cancelPswButton.isEnabled = true
            }
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
                    toolbar.snackbar("Sei collegato come: " + session.user.email).show()
                }
            }
        }
    }
}
