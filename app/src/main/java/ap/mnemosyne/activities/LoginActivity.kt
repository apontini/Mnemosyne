package ap.mnemosyne.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ap.mnemosyne.R

import kotlinx.android.synthetic.main.activity_login.*
import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.material.snackbar.Snackbar
import android.view.View
import ap.mnemosyne.http.HttpHelper
import kotlinx.android.synthetic.main.content_login.*
import okhttp3.Request
import okhttp3.Response
import android.view.inputmethod.InputMethodManager
import ap.mnemosyne.parameters.ParametersHelper
import ap.mnemosyne.resources.Message
import ap.mnemosyne.resources.Resource
import ap.mnemosyne.resources.User
import ap.mnemosyne.session.SessionHelper
import ap.mnemosyne.tasks.TasksHelper
import okhttp3.MediaType
import okhttp3.FormBody
import org.jetbrains.anko.*


class LoginActivity : AppCompatActivity()
{
    private lateinit var session : SessionHelper
    private lateinit var tasks : TasksHelper
    private lateinit var params : ParametersHelper

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        session = SessionHelper(this)
        tasks = TasksHelper(this)
        params = ParametersHelper(this)

        setContentView(R.layout.activity_login)
        setSupportActionBar(toolbar)

        loginButton.setOnClickListener {
            view -> tryLogin()
        }
    }

    fun tryLogin()
    {
        if(loginUser.text.toString() == "" || !android.util.Patterns.EMAIL_ADDRESS.matcher(loginUser.text.toString()).matches())
        {
            loginUser.requestFocus()
            inputLayoutEmail.error = getString(R.string.login_invalidEmail)
            return
        }
        else
        {
            inputLayoutEmail.error = null
        }

        if(loginPassword.text.toString() == "")
        {
            loginPassword.requestFocus()
            inputLayoutPassword.error = getString(R.string.login_invalidPassword)
            return
        }
        else
        {
            inputLayoutPassword.error = null
        }

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
            .url(HttpHelper.AUTH_URL)
            .post(body)
            .build()

        doAsync{
            val response : Pair<Resource?, Response> = HttpHelper(act).request(request, true)

            when(response.second.code())
            {
                201 ->
                {
                    val returnIntent = Intent()
                    session.user = User((response.first as User).sessionID, loginUser.text.toString())
                    tasks.resetLocalTasks()
                    params.resetLocalParameters()
                    setResult(Activity.RESULT_OK, returnIntent)
                    finish()
                }

                HttpHelper.ERROR_PERMISSIONS ->{
                    uiThread {
                        alert(getString(R.string.alert_noInternetPermission)) {  }.show()
                    }
                }

                HttpHelper.ERROR_NO_CONNECTION ->{
                    uiThread {
                        alert(getString(R.string.alert_noInternetConnection)) {  }.show()
                    }
                }

                HttpHelper.ERROR_UNKNOWN ->{
                    uiThread {
                        alert(getString(R.string.alert_generalError)) {  }.show()
                    }
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
                loginProgress.visibility = View.GONE
            }
        }
    }
}
