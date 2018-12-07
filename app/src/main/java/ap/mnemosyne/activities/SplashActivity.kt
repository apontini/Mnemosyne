package ap.mnemosyne.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ap.mnemosyne.http.HttpHelper
import ap.mnemosyne.permissions.PermissionsHelper
import ap.mnemosyne.session.SessionHelper
import ap.mnemosyne.tasks.TasksHelper
import org.jetbrains.anko.alert
import org.jetbrains.anko.okButton

class SplashActivity : AppCompatActivity() {

    private lateinit var tasks: TasksHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tasks = TasksHelper(this)

        doSplash()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            SessionHelper.LOGIN_REQUEST_CODE -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        doSplash()
                    }

                    else -> {
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
                    }
                }
            }
        }
    }

    private fun doSplash()
    {
        tasks.updateTasksAndDo(doWhat = {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_out, android.R.anim.fade_in)
            finish()
        },
            doWhatError = { p0, p1 ->
                if(p0 == HttpHelper.ERROR_NO_CONNECTION)
                {
                    alert("Non c'è connettività, premi Ok per riprovare"){
                        okButton { doSplash() }
                    }.show()
                }
                else if(p0 == HttpHelper.ERROR_PERMISSIONS)
                {
                    PermissionsHelper.askInternetPermission(this)
                }
            })
    }
}