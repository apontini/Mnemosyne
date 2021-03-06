package ap.mnemosyne.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionsHelper
{
    companion object
    {
        private val permissionsRequired = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.RECORD_AUDIO)

        fun askPermissions(act: Activity)
        {
            ActivityCompat.requestPermissions(act, permissionsRequired, 0)
        }

        fun checkPositionPermission(act: Context): Boolean
        {
            return ContextCompat.checkSelfPermission(act,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }

        fun askPositionPermission(act: Activity)
        {
            if (!checkPositionPermission(act))
            {
                ActivityCompat.requestPermissions(act, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
            }
        }

        fun checkCoarsePositionPermission(act: Context): Boolean
        {
            return ContextCompat.checkSelfPermission(act,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }

        fun askCoarsePositionPermission(act: Activity)
        {
            if (!checkCoarsePositionPermission(act))
            {
                ActivityCompat.requestPermissions(act, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 0)
            }
        }

        fun checkInternetPermission(act: Context): Boolean
        {
            return ContextCompat.checkSelfPermission(act,
                    Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED
        }

        fun askInternetPermission(act: Activity)
        {
            if (!checkInternetPermission(act))
            {
                ActivityCompat.requestPermissions(act, arrayOf(Manifest.permission.INTERNET), 0)
            }
        }

        fun checkMicrophonePermission(act: Context): Boolean
        {
            return ContextCompat.checkSelfPermission(act,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        }

        fun askMicrophonePermission(act: Activity)
        {
            if (!checkMicrophonePermission(act))
            {
                ActivityCompat.requestPermissions(act, arrayOf(Manifest.permission.RECORD_AUDIO), 0)
            }
        }
    }
}