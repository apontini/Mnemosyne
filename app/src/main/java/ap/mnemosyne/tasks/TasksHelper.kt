package ap.mnemosyne.tasks

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Base64
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
import org.joda.time.LocalTime
import org.joda.time.Minutes
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.locks.ReentrantReadWriteLock

class TasksHelper(val ctx : Context)
{
    companion object
    {
        val dateTimeFormat : DateTimeFormatter by lazy { return@lazy DateTimeFormat.forPattern("yyyy-MM-dd HH:mm") }
        private const val MIN_UPDATE = 8
        const val LAST_REFRESH : String = "last_refresh"
        val lock by lazy { ReentrantReadWriteLock() }
        val readLock by lazy { lock.readLock() }
        val writeLock by lazy { lock.writeLock() }
    }

    val session = SessionHelper(ctx)

    fun updateTasksAndDo(forceUpdate : Boolean = false, doWhatError: (Int, String) -> Unit = {_,p1 -> ctx.alert(p1).show()}, doWhat: () -> (Unit))
    {
        writeLock.lock()
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
                        val taskmap = mutableMapOf<Int,Task>()
                        (resp.first as ResourceList<Task>).list.forEach{
                            taskmap[it.id] = it
                        }

                        val baos = ByteArrayOutputStream()
                        val oos = ObjectOutputStream(baos)
                        oos.writeObject(taskmap)
                        oos.flush()
                        with(prefs.edit())
                        {
                            putString(ctx.getString(R.string.sharedPreferences_tasks_map), android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT))
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

                    HttpHelper.ERROR_UNKNOWN ->
                    {
                        error = HttpHelper.ERROR_UNKNOWN
                        errorString = ctx.getString(R.string.alert_generalError)
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
        writeLock.unlock()
    }

    fun resetLocalTasks()
    {
        writeLock.lock()
        with(ctx.getSharedPreferences(ctx.getString(R.string.sharedPreferences_tasks_FILE), Context.MODE_PRIVATE).edit())
        {
            remove(ctx.getString(R.string.sharedPreferences_tasks_map))
            putString(LAST_REFRESH, "1970-01-01 00:00")
            apply()
        }

        writeLock.unlock()
    }

    fun removeLocalTasks(t : Task)
    {
        writeLock.lock()
        val mapString = ctx.getSharedPreferences(ctx.getString(R.string.sharedPreferences_tasks_FILE), Context.MODE_PRIVATE)
            .getString(ctx.getString(R.string.sharedPreferences_tasks_map), "")?.toByteArray(StandardCharsets.UTF_8)
            ?: "".toByteArray(StandardCharsets.UTF_8)

        try
        {
            val bais = ByteArrayInputStream(Base64.decode(mapString, Base64.DEFAULT))
            val inputStream = ObjectInputStream(bais)
            val map = inputStream.readObject() as MutableMap<Int, Task>
            map.remove(t.id)
            val baos = ByteArrayOutputStream()
            val oos = ObjectOutputStream(baos)
            oos.writeObject(map)
            oos.flush()
            with(ctx.getSharedPreferences(ctx.getString(R.string.sharedPreferences_tasks_FILE), Context.MODE_PRIVATE).edit())
            {
                putString(ctx.getString(R.string.sharedPreferences_tasks_map), android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT))
                apply()
            }
        }
        catch (e : java.lang.Exception)
        {
            e.printStackTrace()
        }
        finally
        {
            writeLock.unlock()
        }
    }

    fun modifyLocalTasks(t : Task)
    {
        writeLock.lock()
        val mapString = ctx.getSharedPreferences(ctx.getString(R.string.sharedPreferences_tasks_FILE), Context.MODE_PRIVATE)
            .getString(ctx.getString(R.string.sharedPreferences_tasks_map), "")?.toByteArray(StandardCharsets.UTF_8)
            ?: "".toByteArray(StandardCharsets.UTF_8)

        try
        {
            val bais = ByteArrayInputStream(Base64.decode(mapString, Base64.DEFAULT))
            val inputStream = ObjectInputStream(bais)
            val map = inputStream.readObject() as MutableMap<Int, Task>
            map[t.id] = t
            val baos = ByteArrayOutputStream()
            val oos = ObjectOutputStream(baos)
            oos.writeObject(map)
            oos.flush()
            with(ctx.getSharedPreferences(ctx.getString(R.string.sharedPreferences_tasks_FILE), Context.MODE_PRIVATE).edit())
            {
                putString(ctx.getString(R.string.sharedPreferences_tasks_map), android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT))
                apply()
            }
        }
        catch (e : java.lang.Exception)
        {
            e.printStackTrace()
        }
        finally
        {
            writeLock.unlock()
        }
    }

    fun getLocalTasks() : Map<Int, Task>?
    {
        readLock.lock()
        val mapString = ctx.getSharedPreferences(ctx.getString(R.string.sharedPreferences_tasks_FILE), Context.MODE_PRIVATE)
            .getString(ctx.getString(R.string.sharedPreferences_tasks_map), "")?.toByteArray(StandardCharsets.UTF_8)
            ?: "".toByteArray(StandardCharsets.UTF_8)

        return try
        {
            val bais = ByteArrayInputStream(Base64.decode(mapString, Base64.DEFAULT))
            val inputStream = ObjectInputStream(bais)
            val map = inputStream.readObject() as MutableMap<Int, Task>
            map
        }
        catch (e : java.lang.Exception)
        {
            e.printStackTrace()
            null
        }
        finally
        {
            readLock.unlock()
        }
    }

    fun getLocalTask(id : Int) : Task?
    {
        readLock.lock()
        val mapString = ctx.getSharedPreferences(ctx.getString(R.string.sharedPreferences_tasks_FILE), Context.MODE_PRIVATE)
            .getString(ctx.getString(R.string.sharedPreferences_tasks_map), "")?.toByteArray(StandardCharsets.UTF_8)
            ?: "".toByteArray(StandardCharsets.UTF_8)

        return try
        {
            val bais = ByteArrayInputStream(Base64.decode(mapString, Base64.DEFAULT))
            val inputStream = ObjectInputStream(bais)
            val map = inputStream.readObject() as MutableMap<Int, Task>
            map[id]
        }
        catch (e : java.lang.Exception)
        {
            e.printStackTrace()
            null
        }
        finally
        {
            readLock.unlock()
        }
    }
}