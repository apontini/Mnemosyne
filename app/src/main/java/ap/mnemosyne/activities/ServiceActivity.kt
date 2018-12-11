package ap.mnemosyne.activities

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import ap.mnemosyne.R
import ap.mnemosyne.permissions.PermissionsHelper
import ap.mnemosyne.services.HintsService

class ServiceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service)
        val action = intent.getIntExtra("ACTION", -1)
        val pintent = intent.extras?.getParcelable<PendingIntent>("PIntent")
        when(action)
        {
            HintsService.ACTION_ASK_POSITION_PERMISSION->
            {
                PermissionsHelper.askPositionPermission(this)
            }

            HintsService.ACTION_ASK_COARSE_POSITION_PERMISSION->
            {
                PermissionsHelper.askCoarsePositionPermission(this)
            }

            HintsService.ACTION_ACTIVATE_POSITION,HintsService.ACTION_CHANGE_POSITION_SETTINGS ->
            {
                if(pintent != null) startIntentSenderForResult (pintent.intentSender,1235, null, 0, 0, 0)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        if(requestCode == 0)
        {
            val notGranted = grantResults.filter { it != PackageManager.PERMISSION_GRANTED }
            if(!notGranted.isEmpty())
            {
                PermissionsHelper.askPermissions(this)
            }
            else
            {
                val serviceIntent = Intent(this, HintsService::class.java)
                serviceIntent.action = HintsService.START
                startService(serviceIntent)
                finish()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode)
        {
            1235 ->
            {
                if(resultCode == Activity.RESULT_OK)
                {
                    val serviceIntent = Intent(this, HintsService::class.java)
                    serviceIntent.action = HintsService.START
                    startService(serviceIntent)
                    finish()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
