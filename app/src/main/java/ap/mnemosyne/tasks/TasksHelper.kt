package ap.mnemosyne.tasks

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import ap.mnemosyne.activities.LoginActivity
import ap.mnemosyne.http.HttpHelper
import ap.mnemosyne.session.SessionHelper
import ap.mnemosyne.R
import ap.mnemosyne.resources.ResourceList
import ap.mnemosyne.resources.Task
import okhttp3.Request
import org.jetbrains.anko.alert
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.joda.time.LocalDateTime
import org.joda.time.Minutes
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.util.concurrent.locks.ReentrantLock

class TasksHelper(val ctx : Context)
{
    companion object
    {
        val dateTimeFormat : DateTimeFormatter by lazy { return@lazy DateTimeFormat.forPattern("yyyy-MM-dd HH:mm") }
        private const val MIN_UPDATE = 8
        const val LAST_REFRESH : String = "last_refresh"
        val lock by lazy { ReentrantLock() }
    }

    val session = SessionHelper(ctx)

    fun updateTasksAndDo(forceUpdate : Boolean = false, doWhatError: (Int, String) -> Unit = {_,p1 -> ctx.alert(p1).show()}, doWhat: () -> (Unit))
    {
        lock.lock()
        val refreshed = LocalDateTime.parse(ctx.getSharedPreferences(ctx.getString(R.string.sharedPreferences_tasks_FILE), Context.MODE_PRIVATE).getString(LAST_REFRESH,
            "1970-01-01 00:00"), dateTimeFormat)
        val now = LocalDateTime.now()
        Log.d("LOCK", refreshed.toString())
        if (forceUpdate || Minutes.minutesBetween(refreshed, now).minutes >= MIN_UPDATE)
        {
            Log.d("TASKS", "Requesting Tasks Update")
            doAsync {
                val request = Request.Builder()
                    .addHeader("Cookie", "JSESSIONID=" + session.user.sessionID)
                    .url(HttpHelper.REST_TASK_URL)
                    .build()
                val resp = HttpHelper(ctx).request(request, true)
                var error = 0
                var errorString = ""
                when (resp.second.code())
                {
                    401 ->
                    {
                        error = 401
                        Log.d("SESSION", "Sessione scaduta")
                        val intent = Intent(ctx, LoginActivity::class.java)
                        if(ctx is Activity) ctx.startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
                    }

                    200 ->
                    {
                        val prefs = ctx.getSharedPreferences(ctx.getString(R.string.sharedPreferences_tasks_FILE), Context.MODE_PRIVATE)
                        with(prefs.edit())
                        {
                            putString(ctx.getString(R.string.sharedPreferences_tasks_list), (resp.first as ResourceList<Task>).toJSON())
                            apply()
                        }

                        with(prefs.edit()) {
                            putString(LAST_REFRESH, now.toString(dateTimeFormat))
                            apply()
                        }
                    }

                    HttpHelper.ERROR_PERMISSIONS ->
                    {
                        error = HttpHelper.ERROR_PERMISSIONS
                        errorString = ctx.getString(R.string.alert_noInternetPermission)
                    }

                    HttpHelper.ERROR_NO_CONNECTION ->
                    {
                        error = HttpHelper.ERROR_NO_CONNECTION
                        errorString = ctx.getString(R.string.alert_noInternetConnection)
                    }

                    else ->
                    {
                        error = -1
                        uiThread {ctx.alert("Non ho potuto aggiornare i task, codice: " + resp.second.code()) { }.show()  }
                    }
                }
                if (error == 0)
                {
                    uiThread {
                        doWhat()
                    }
                }
                else if(error > 0)
                {
                    uiThread {
                        doWhatError(error, errorString)
                    }
                }
            }
        }
        else
        {
            Log.d("TASK", "No need to update")
            doWhat()
        }
        lock.unlock()
    }

    fun resetLocalTasks()
    {
        lock.lock()
        with(ctx.getSharedPreferences(ctx.getString(R.string.sharedPreferences_tasks_FILE), Context.MODE_PRIVATE).edit())
        {
            remove(ctx.getString(R.string.sharedPreferences_tasks_list))
            putString(LAST_REFRESH, "1970-01-01 00:00")
            apply()
        }

        lock.unlock()
    }

    fun removeLocalTasks(t : Task)
    {
        lock.lock()
        val list = ResourceList.fromJSON(ctx.getSharedPreferences(ctx.getString(R.string.sharedPreferences_tasks_FILE), Context.MODE_PRIVATE)
                                            .getString(ctx.getString(R.string.sharedPreferences_tasks_list), "")).list as MutableList<Task>
        list.remove(t)

        with(ctx.getSharedPreferences(ctx.getString(R.string.sharedPreferences_tasks_FILE), Context.MODE_PRIVATE).edit())
        {
            putString(ctx.getString(R.string.sharedPreferences_tasks_list), ResourceList<Task>(list).toJSON())
            apply()
        }

        lock.unlock()
    }

    fun updateLocalTasks(t : Task)
    {
        lock.lock()
        val list = ResourceList.fromJSON(ctx.getSharedPreferences(ctx.getString(R.string.sharedPreferences_tasks_FILE), Context.MODE_PRIVATE)
            .getString(ctx.getString(R.string.sharedPreferences_tasks_list), "")).list as MutableList<Task>
        val index = list.indexOf(t)
        list[index] = t

        with(ctx.getSharedPreferences(ctx.getString(R.string.sharedPreferences_tasks_FILE), Context.MODE_PRIVATE).edit())
        {
            putString(ctx.getString(R.string.sharedPreferences_tasks_list), ResourceList<Task>(list).toJSON())
            apply()
        }

        lock.unlock()
    }

    fun getLocalTasks() : Iterable<Task>?
    {
        lock.lock()
        return try
        {
            ResourceList.fromJSON(
                ctx.getSharedPreferences(ctx.getString(R.string.sharedPreferences_tasks_FILE), Context.MODE_PRIVATE)
                    .getString(ctx.getString(R.string.sharedPreferences_tasks_list), "")).list as List<Task>
        }
        catch (e: Exception)
        {
            null
        }
        finally
        {
            lock.unlock()
        }
    }

    fun getLocalTask(id : Int) : Task?
    {
        lock.lock()
        return try
        {
            val list = ResourceList.fromJSON(
                ctx.getSharedPreferences(ctx.getString(R.string.sharedPreferences_tasks_FILE), Context.MODE_PRIVATE)
                    .getString(ctx.getString(R.string.sharedPreferences_tasks_list), "")).list as List<Task>
            val filtered = list.filter { it.id == id }
            if(filtered.isEmpty())
                null
            else
                filtered.first()
        }
        catch (e: Exception)
        {
            null
        }
        finally
        {
            lock.unlock()
        }
    }
}