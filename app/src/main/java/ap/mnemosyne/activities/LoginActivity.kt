package ap.mnemosyne.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import apontini.mnemosyne.R

import kotlinx.android.synthetic.main.activity_login.*
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.support.annotation.UiThread
import android.support.design.widget.Snackbar
import android.view.View
import ap.mnemosyne.httphandler.HttpHandler
import kotlinx.android.synthetic.main.content_login.*
import okhttp3.Request
import okhttp3.Response
import ap.mnemosyne.resources.Message
import ap.mnemosyne.resources.Resource
import android.view.inputmethod.InputMethodManager
import ap.mnemosyne.resources.User
import ap.mnemosyne.session.SessionManager
import okhttp3.MediaType
import okhttp3.FormBody
import org.jetbrains.anko.*


class LoginActivity : AppCompatActivity()
{
    private lateinit var session : SessionManager

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        session = SessionManager(this)

        setContentView(R.layout.activity_login)
        setSupportActionBar(toolbar)

        loginButton.setOnClickListener {
            view -> tryLogin()
        }
    }

    fun tryLogin()
    {
        val mgr = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        loginUser.isEnabled = false
        loginPassword.isEnabled = false
        loginButton.isEnabled = false
        loginProgress.visibility = View.VISIBLE

        mgr!!.hideSoftInputFromWindow(layout_login.windowToken, 0)
        val act = this
        val mtype = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8")
        val body = FormBody.Builder().add("email", loginUser.text.toString()).add("password", loginPassword.text.toString()).build()
        val request = Request.Builder()
            .url(HttpHandler.AUTH_URL)
            .post(body)
            .build()

        doAsync{
            val response : Pair<Resource?, Response> = HttpHandler(act).request(request, true)

            when(response.second.code())
            {
                201 ->
                {
                    val returnIntent = Intent()
                    session.user = User((response.first as User).sessionID, loginUser.text.toString())
                    setResult(Activity.RESULT_OK, returnIntent)
                    finish()
                }

                999 ->{
                    alert(getString(R.string.alert_noInternetPermission)) {  }.show()
                }

                else -> {
                    Snackbar.make(findViewById(R.id.layout_login), response.second.code().toString() + " " + (response.first as Message).errorDetails, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
                }
            }
            uiThread {
                loginUser.isEnabled = true
                loginPassword.isEnabled = true
                loginButton.isEnabled = true
                loginProgress.visibility = View.INVISIBLE
            }
        }
    }
}
