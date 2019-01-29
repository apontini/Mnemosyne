package ap.mnemosyne.services

import android.content.Intent
import android.app.IntentService
import android.app.NotificationManager
import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import ap.mnemosyne.R
import ap.mnemosyne.http.HttpHelper
import ap.mnemosyne.resources.*
import ap.mnemosyne.tasks.TasksHelper
import okhttp3.*
import org.jetbrains.anko.doAsync

class HintsHelperService : IntentService("HintsHelperService")
{
    companion object
    {
        const val ACTION_SNOOZE_MIN = "SNOOZE_MIN"
        const val ACTION_SNOOZE_MAX = "SNOOZE_MAX"
        const val ACTION_COMPLETED_SUCCESS = "COMPLETED_SUCCESS"
        const val ACTION_COMPLETED_FAILED = "COMPLETED_FAILED"
        const val SNOOZE_MIN_MINUTES = 15
        const val SNOOZE_MAX_MINUTES = 60
    }

    private lateinit var tasks : TasksHelper

    override fun onHandleIntent(intent: Intent?)
    {
        val id = intent?.getIntExtra("taskID",-1) ?: -1
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(id)
        Log.d("SERVICE-INTENT", "TaskID: $id, action: ${intent?.action ?: ""}")
        when(intent?.action ?: "")
        {
            ACTION_SNOOZE_MAX ->
            {
                if(id != -1) SnoozeHelper(this).snoozeFor(id, SNOOZE_MAX_MINUTES)
            }

            ACTION_SNOOZE_MIN ->
            {
                if(id != -1) SnoozeHelper(this).snoozeFor(id, SNOOZE_MIN_MINUTES)
            }

            ACTION_COMPLETED_FAILED ->
            {
                if(id != -1)
                {
                    tasks = TasksHelper(this)
                    setTaskBooleans(id, true, false)
                }
            }

            ACTION_COMPLETED_SUCCESS ->
            {
                if(id != -1)
                {
                    tasks = TasksHelper(this)
                    setTaskBooleans(id, false, true)
                }
            }
        }
    }

    fun setTaskBooleans(id : Int, failed : Boolean, done : Boolean)
    {
        if(failed == done) return
        val sessionid = getSharedPreferences(getString(R.string.sharedPreferences_user_FILE), Context.MODE_PRIVATE)
            .getString(getString(R.string.sharedPreferences_user_sessionid), "") ?: ""

        val oldTask = tasks.getLocalTask(id) ?: return
        val task = Task(oldTask.id, oldTask.user, oldTask.name, oldTask.constr, oldTask.isPossibleAtWork, oldTask.isCritical, oldTask.isRepeatable, done, failed, oldTask.isIgnoredToday, oldTask.placesToSatisfy)

        val request = Request.Builder()
            .addHeader("Cookie" , "JSESSIONID=" + sessionid)
            .url(HttpHelper.REST_TASK_URL)
            .put(RequestBody.create(MediaType.parse("application/json"), task.toJSON()))
            .build()

        doAsync {
            val response : Pair<Resource?, Response> = HttpHelper(this@HintsHelperService).request(request, true)
            when(response.second.code())
            {
                200->
                {
                    tasks.modifyLocalTasks(task)
                    Log.d("SERVICE-INTENT", "Task updated")
                }

                401->
                {
                    val respMessage = response.first as Message
                    createTextNotification(Resources.getSystem().getString(R.string.error), respMessage.message)
                }

                400->
                {
                    val respMessage = response.first as Message
                    createTextNotification(Resources.getSystem().getString(R.string.error), respMessage.message)
                }

                500 ->
                {
                    val respMessage = response.first as Message
                    createTextNotification(Resources.getSystem().getString(R.string.error), respMessage.message)
                }
            }
        }
    }

    fun createTextNotification(title: String, text : String)
    {
        val notification = NotificationCompat.Builder(this, HintsService.CHANNEL_ID).apply {
            setContentTitle(title)
            setContentText(text)
            val bigTextStyle = NotificationCompat.BigTextStyle()
            with(bigTextStyle) {
                setBigContentTitle(title)
                bigText(text)
            }
            setStyle(bigTextStyle)
            setWhen(System.currentTimeMillis())
            priority = NotificationCompat.PRIORITY_MAX
            setSmallIcon(R.mipmap.ic_mnemosyne_notif)
            val largeIconBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_mnemosyne_launcher)
            setLargeIcon(largeIconBitmap)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setChannelId(HintsService.CHANNEL_ID)
        }

        with(getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager) {
            notify(HintsService.TEXT_NOTIFICATION_ID, notification.build())
        }
    }
}