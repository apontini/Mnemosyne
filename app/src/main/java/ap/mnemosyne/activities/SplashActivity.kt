package ap.mnemosyne.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ap.mnemosyne.session.SessionHelper
import ap.mnemosyne.tasks.TasksHelper

class SplashActivity : AppCompatActivity()
{

    private lateinit var tasks : TasksHelper

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        tasks = TasksHelper(this)

        tasks.updateTasksAndDo {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        when(requestCode)
        {
            SessionHelper.LOGIN_REQUEST_CODE->{
                when(resultCode)
                {
                    Activity.RESULT_OK ->
                    {
                        tasks.updateTasksAndDo {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                    }

                    else ->
                    {
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
                    }
                }
            }
        }
    }
}