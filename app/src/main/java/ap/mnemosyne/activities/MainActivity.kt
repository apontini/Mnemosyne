package ap.mnemosyne.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity;
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import ap.mnemosyne.permissions.PermissionsHelper
import ap.mnemosyne.session.SessionHelper
import apontini.mnemosyne.R

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.design.snackbar
import android.view.animation.AnimationUtils


class MainActivity : AppCompatActivity()
{

    private lateinit var session : SessionHelper
    private var isViewCreated = false

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        Log.d("LIFECYCLE", "onCreate")
        session = SessionHelper(this)
        session.checkSessionValidity {
            isViewCreated = true
            createContentView() }

        //Permissions check
        PermissionsHelper.askPermissions(this)
    }

    private fun createContentView()
    {
        setContentView(R.layout.activity_main)

        snackbar(findViewById(R.id.layout_main), "Sei collegato come: " + session.user.email).show()

        listButton.setOnClickListener {
            val intent = Intent(this, TaskListActivity::class.java)
            startActivity(intent)
        }

        addButton.setOnClickListener {
                view -> snackbar(view, "Non implementato")
        }

        micButton.setOnClickListener {
            val intent = Intent(this, VoiceActivity::class.java)
            intent.putExtra("sessionid", session.user.sessionID)
            startActivityForResult(intent, 1)
        }

        settingsButton.setOnClickListener{
            val animation = AnimationUtils.loadAnimation(this, R.anim.spin_around_itself)
            settingsButton.startAnimation(animation)
            val intent = Intent(this, SettingsActivity::class.java)
            startActivityForResult(intent, 2)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId)
        {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
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
                        if(!isViewCreated)
                        {
                            createContentView()
                        }
                        else
                        {
                            snackbar(findViewById(R.id.layout_main), "Sei collegato come: " + session.user.email).show()
                            isViewCreated = false
                        }
                    }

                    else -> {
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
                    }
                }
            }

           1->{
               when(resultCode)
               {
                   Activity.RESULT_OK -> {
                       val intent = Intent(this, TaskDetailsActivity::class.java)
                       intent.putExtra("task", data?.getSerializableExtra("resultTask"))
                       startActivity(intent)
                   }
               }
           }

           2->{
               when(resultCode)
               {
                   SettingsActivity.LOGOUT_RESULT_CODE -> {
                       val intent = Intent(this, LoginActivity::class.java)
                       startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
                       isViewCreated = true
                   }
               }
           }
       }
    }
}
