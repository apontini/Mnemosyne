package ap.mnemosyne.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import ap.mnemosyne.services.HintsService

class AlarmReceiver : BroadcastReceiver()
{
    override fun onReceive(context: Context?, intent: Intent?)
    {
        Log.d("ALARM", "Alarm Received")
        val serviceIntent = Intent(context, HintsService::class.java)
        serviceIntent.action = HintsService.CHECK_HINTS
        context?.startService(serviceIntent)
    }
}
