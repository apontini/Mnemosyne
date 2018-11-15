package ap.mnemosyne.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import apontini.mnemosyne.R

import kotlinx.android.synthetic.main.activity_login.*
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.support.design.widget.Snackbar
import ap.mnemosyne.httphandler.HttpHandler
import kotlinx.android.synthetic.main.content_login.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import ap.mnemosyne.resources.Message
import ap.mnemosyne.resources.Resource
import com.fasterxml.jackson.core.JsonParseException
import java.io.ByteArrayInputStream
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import ap.mnemosyne.resources.User
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.FormBody
import org.jetbrains.anko.*
import java.lang.ClassCastException


class LoginActivity : AppCompatActivity()
{

    private val loginUrl : String = "http://pintini.ddns.net:8080/mnemosyne/auth"

    lateinit var mail : EditText
    lateinit var psw : EditText

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setSupportActionBar(toolbar)

        mail = findViewById(R.id.loginUser) as EditText
        psw = findViewById(R.id.loginPassword) as EditText

        loginButton.setOnClickListener {
            view -> tryLogin()
        }
    }

    fun tryLogin()
    {
        val mgr = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        mgr!!.hideSoftInputFromWindow(findViewById(R.id.layout_login).windowToken, 0)
        val act = this

        doAsync{
            val mtype = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8")
            val body = FormBody.Builder().add("email", mail.text.toString()).add("password", psw.text.toString()).build()
            val client : OkHttpClient = HttpHandler(act).getClient()
            val request = Request.Builder()
                .url(loginUrl)
                .post(body)
                .build()

            val response : Response = client.newCall(request).execute()
            val standard = Message("Error", "", "(Error details not specified by the server)")
            val cceMess = Message("Error", "", "I Didn't understant what the server told measd")
            val bodyResp : String = response.body()?.string() ?: standard.toJSON()
            val stream = ByteArrayInputStream(bodyResp.toByteArray(Charsets.UTF_8))

            when(response.code())
            {
                201 -> {
                    val u = Resource.fromJSON(stream) as User
                    uiThread {
                        //Update the UI thread here
                        alert(bodyResp, "message") {
                            yesButton { toast("Yay !") }
                            noButton { toast(":( !") }
                        }.show()
                    }
                    /*val returnIntent = Intent()
                    setResult(Activity.RESULT_OK, returnIntent)
                    finish()*/
                }

                else -> {
                    val m : Message = try{ Resource.fromJSON(stream) as Message } catch (jpe : JsonParseException) { standard } catch (cce : ClassCastException) { cceMess }
                    Snackbar.make(findViewById(R.id.layout_login), response.code().toString() + " " + m.errorDetails, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
                }

            }
        }

    }
}
