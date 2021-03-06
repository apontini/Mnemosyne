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
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import ap.mnemosyne.R
import ap.mnemosyne.enums.ParamsName
import ap.mnemosyne.parameters.ParametersHelper
import ap.mnemosyne.services.HintsService


class SplashActivity : AppCompatActivity() {

    private lateinit var tasks: TasksHelper
    private lateinit var params: ParametersHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tasks = TasksHelper(this)
        params = ParametersHelper(this)
        //Permissions check
        PermissionsHelper.askPermissions(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            OnboardingActivity.ONBOARDING_REQUEST_CODE ->
            {
                when (resultCode)
                {
                    Activity.RESULT_OK ->
                    {
                        doSplash()
                    }
                }
            }

            SessionHelper.LOGIN_REQUEST_CODE ->
            {
                when (resultCode)
                {
                    Activity.RESULT_OK ->
                    {
                        doSplash()
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
                doSplash()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun doSplash()
    {
        params.updateParametersAndDo(true,
            doWhat = {
                var foundNull = false
                if(params.getLocalParameter(ParamsName.time_lunch) == null ||
                    params.getLocalParameter(ParamsName.time_dinner) == null ||
                    params.getLocalParameter(ParamsName.time_bed) == null ||
                    params.getLocalParameter(ParamsName.time_work) == null ||
                    params.getLocalParameter(ParamsName.location_house) == null ||
                    params.getLocalParameter(ParamsName.location_work) == null)
                {
                    foundNull = true
                }

                if(!foundNull)
                {
                    tasks.updateTasksAndDo(true, doWhat = {
                        val service = HintsService()
                        if (!isMyServiceRunning(service.javaClass))
                        {
                            val serviceIntent = Intent(this, service.javaClass)
                            serviceIntent.action = HintsService.START
                            startService(serviceIntent)
                        }
                        startActivity(Intent(this, MainActivity::class.java))
                        overridePendingTransition(android.R.anim.fade_out, android.R.anim.fade_in)
                        finish()
                    },
                        doWhatError = { p0, p1 -> solveError(p0, p1) })
                }
                else
                {
                    val onboardingIntent = Intent(this, OnboardingActivity::class.java)
                    startActivityForResult(onboardingIntent, OnboardingActivity.ONBOARDING_REQUEST_CODE)
                }
            },
            doWhatError = { p0, p1 -> solveError(p0,p1)})
    }

    private fun solveError(p0 : Int, p1 : String)
    {
        when (p0)
        {
            HttpHelper.ERROR_NO_CONNECTION -> alert(getString(R.string.alert_noConnectivitySplash)){
                okButton { doSplash() }
            }.show()

            HttpHelper.ERROR_PERMISSIONS -> PermissionsHelper.askInternetPermission(this)

            HttpHelper.ERROR_UNKNOWN -> alert(getString(R.string.alert_generalError)){
                okButton { doSplash() }
            }.show()
        }
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean
    {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE))
        {
            if (serviceClass.name == service.service.className)
            {
                Log.i("isMyServiceRunning?", "true")
                return true
            }
        }
        Log.i("isMyServiceRunning?", "false")
        return false
    }
}