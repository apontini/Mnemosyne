package ap.mnemosyne.services

import android.content.Intent
import android.app.IntentService
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import org.joda.time.LocalTime


class HintsHelperService : IntentService("HintsHelperService")
{
    companion object
    {
        const val ACTION_SNOOZE_MIN = "SNOOZE_MIN"
        const val ACTION_SNOOZE_MAX = "SNOOZE_MAX"
        const val SNOOZE_MIN_MINUTES = 1
        const val SNOOZE_MAX_MINUTES = 2
    }

    override fun onHandleIntent(intent: Intent?)
    {
        val snoozed = SnoozeHelper(this)
        val id = intent?.getIntExtra("taskID",-1) ?: -1
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(id)
        Log.d("SERVICE-INTENT", "TaskID: $id, action: ${intent?.action ?: ""}")
        when(intent?.action ?: "")
        {
            ACTION_SNOOZE_MAX ->
            {
                if(id != -1) snoozed.snoozeFor(id, SNOOZE_MAX_MINUTES)
            }

            ACTION_SNOOZE_MIN ->
            {
                if(id != -1) snoozed.snoozeFor(id, SNOOZE_MIN_MINUTES)
            }
        }
    }
}