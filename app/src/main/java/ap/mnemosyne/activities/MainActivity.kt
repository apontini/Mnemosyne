package ap.mnemosyne.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity;
import android.view.Menu
import android.view.MenuItem
import ap.mnemosyne.httphandler.HttpHandler
import ap.mnemosyne.resources.User
import apontini.mnemosyne.R

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import okhttp3.Request
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val sharedPref = this.getSharedPreferences(getString(R.string.sharedPreferences_user_FILE), Context.MODE_PRIVATE)

        val sessionid = sharedPref.getString(getString(R.string.sharedPreferences_user_sessionid), "")
        val useremail = sharedPref.getString(getString(R.string.sharedPreferences_user_mail),"")
        val thisActivity = this
        if(sessionid == "" || useremail == "")
        {
            val intent = Intent(this, LoginActivity::class.java)
            startActivityForResult(intent, 0)
        }
        else
        {
            val request = Request.Builder()
                .addHeader("Cookie" , "JSESSIONID="+sessionid)
                .url(HttpHandler.REST_USER_URL)
                .build()
            doAsync {
                val resp = HttpHandler(thisActivity).request(request, true)
                when(resp.second.code())
                {
                    401 -> {
                        val intent = Intent(thisActivity, LoginActivity::class.java)
                        startActivityForResult(intent, 0)
                    }

                    200 -> {
                        snackbar(findViewById(R.id.layout_main), "Sei collegato come: " + (resp.first as User).email).show()
                    }
                }
            }
        }

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        listButton.setOnClickListener {
            view -> snackbar(view, "Non implementato")
        }

        addButton.setOnClickListener {
                view -> snackbar(view, "Non implementato")
        }

         micButton.setOnClickListener {
                view -> snackbar(view, "Non implementato")
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
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        if(requestCode == 0)
        {
            when(resultCode)
            {
                Activity.RESULT_OK -> {
                    snackbar(findViewById(R.id.layout_main), "Sei collegato come: " + data?.getStringExtra("mail") ?: "null").show()
                    val pref : SharedPreferences = this.getSharedPreferences(getString(R.string.sharedPreferences_user_FILE), Context.MODE_PRIVATE)
                    with(pref.edit())
                    {
                        putString(getString(R.string.sharedPreferences_user_sessionid), data?.getStringExtra(getString(R.string.sharedPreferences_user_sessionid)))
                        putString(getString(R.string.sharedPreferences_user_mail), data?.getStringExtra(getString(R.string.sharedPreferences_user_mail)))
                        putString(getString(R.string.sharedPreferences_user_psw), data?.getStringExtra(getString(R.string.sharedPreferences_user_psw)))
                        if(!commit()) toast("WOPS").show()
                    }

                }

                else -> {
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivityForResult(intent, 0)
                }
            }
        }
    }
}
