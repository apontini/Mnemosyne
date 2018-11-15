package ap.mnemosyne.httphandler

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import okhttp3.OkHttpClient

class HttpHandler(act: Activity)
{
    companion object
    {
        val httpclient : OkHttpClient by lazy { OkHttpClient() }
    }

    init
    {
        checkNetworkPermission(act)
    }

    fun getClient() : OkHttpClient
    {
        return httpclient
    }

    private fun checkNetworkPermission(act: Activity)
    {
        while (ContextCompat.checkSelfPermission(act, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
        {
            val dlgAlert = AlertDialog.Builder(act)
            dlgAlert.setMessage("Please allow us to access to the internet")
            dlgAlert.setTitle("Mnemosyne")
            dlgAlert.setPositiveButton("Ok")
            {
                    dialog, which ->
            }
            dlgAlert.setCancelable(true)
            dlgAlert.create().show()
            ActivityCompat.requestPermissions(act, arrayOf(Manifest.permission.INTERNET), 0)
        }
    }


}
