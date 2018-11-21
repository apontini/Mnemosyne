package ap.mnemosyne.activities

import android.os.Bundle
import android.preference.Preference
import apontini.mnemosyne.R
import android.preference.PreferenceFragment
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.alert


class SettingsActivity : AppCompatActivity()
{

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fragmentManager
            .beginTransaction()
            .replace(R.id.settingsLayout, Preferences())
            .commit()
    }

    class Preferences : PreferenceFragment()
    {
        override fun onCreate(savedInstanceState: Bundle?)
        {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)

            val myPref = findPreference("key4") as Preference
            myPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {

                alert("asd") {  }.show()
                true
            }
        }


    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId)
        {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
