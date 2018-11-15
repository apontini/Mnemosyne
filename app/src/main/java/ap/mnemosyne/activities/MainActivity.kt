package ap.mnemosyne.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity;
import android.view.Menu
import android.view.MenuItem
import apontini.mnemosyne.R

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val sharedPref = this.getSharedPreferences(getString(R.string.user_preferences), Context.MODE_PRIVATE)

        val sessionid : String = sharedPref.getString("JSESSIONID", "")
        if(sessionid == "")
        {
            val intent = Intent(this, LoginActivity::class.java)
            startActivityForResult(intent, 0);
        }

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
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
                Activity.RESULT_OK -> {val text : String = "Ok"
                    Snackbar.make(findViewById(R.id.layout_main), text, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()}
                else -> {val intent = Intent(this, LoginActivity::class.java)
                    startActivityForResult(intent, 0);}
            }
        }
    }
}
