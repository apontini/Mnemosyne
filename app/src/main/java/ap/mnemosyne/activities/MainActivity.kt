package ap.mnemosyne.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity;
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import ap.mnemosyne.httphandler.HttpHandler
import ap.mnemosyne.permissions.PermissionsHandler
import ap.mnemosyne.resources.User
import ap.mnemosyne.session.SessionManager
import apontini.mnemosyne.R

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import okhttp3.Request
import org.jetbrains.anko.alert
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class MainActivity : AppCompatActivity()
{

    private lateinit var session : SessionManager
    private lateinit var thisActivity : Activity

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        session = SessionManager(this)

        //Permissions check
        PermissionsHandler.askPermissions(this)

        val sessionid = session.user.sessionID
        val useremail = session.user.email
        thisActivity = this

        if(sessionid == "" || useremail == "")
        {
            Log.d("SESSION", "Preferences non trovate")
            val intent = Intent(this, LoginActivity::class.java)
            startActivityForResult(intent, 0)
        }
        else
        {
            val request = Request.Builder()
                .addHeader("Cookie" , "JSESSIONID="+sessionid)
                .url(HttpHandler.REST_USER_URL)
                .build()
            var error = false
            doAsync {
                val resp = HttpHandler(thisActivity).request(request, true)
                when(resp.second.code())
                {
                    401 -> {
                        Log.d("SESSION", "Sessione scaduta")
                        val intent = Intent(thisActivity, LoginActivity::class.java)
                        startActivityForResult(intent, 0)
                    }

                    200 -> {
                        Log.d("SESSION", "Sessione valida")
                        snackbar(findViewById(R.id.layout_main), "Sei collegato come: " + (resp.first as User).email).show()
                    }

                    999 ->{
                        alert(getString(R.string.alert_noInternetPermission)) {  }.show()
                        error = true
                    }

                    else ->{
                        snackbar(findViewById(R.id.layout_main), resp.second.code()).show()
                    }
                }
                uiThread {
                    if(!error)
                    {
                        listButton.isClickable = true
                        addButton.isClickable = true
                        micButton.isClickable = true
                    }
                }
            }
        }

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        listButton.setOnClickListener {
            val intent = Intent(thisActivity, TaskListActivity::class.java)
            startActivity(intent)
        }

        addButton.setOnClickListener {
                view -> snackbar(view, "Non implementato")
        }

         micButton.setOnClickListener {
             val intent = Intent(thisActivity, VoiceActivity::class.java)
             intent.putExtra("sessionid", sessionid)
             startActivityForResult(intent, 1)
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
                val intent = Intent(thisActivity, SettingsActivity::class.java)
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
            0->{
                when(resultCode)
                {
                    Activity.RESULT_OK -> {
                        snackbar(findViewById(R.id.layout_main), "Sei collegato come: " + session.user.email).show()
                        listButton.isClickable = true
                        addButton.isClickable = true
                        micButton.isClickable = true
                    }

                    else -> {
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivityForResult(intent, 0)
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
       }
    }
}
