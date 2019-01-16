package ap.mnemosyne.services

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import ap.mnemosyne.R
import org.joda.time.LocalTime
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.Exception
import java.nio.charset.StandardCharsets
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class SnoozeHelper(private val ctx : Context)
{
    companion object
    {
        private val lock by lazy{ReentrantReadWriteLock()}
        private val SNOOZED_PREF = "snoozed_tasks"
        private var map = mutableMapOf<Int, LocalTime>()
    }

    private var spref : SharedPreferences =
        ctx.getSharedPreferences(ctx.getString(R.string.sharedPreferences_tasks_FILE), Context.MODE_PRIVATE)

    private var stat : SharedPreferences =
        ctx.getSharedPreferences("apontini.mnemosyne.statsSharedPrefs", Context.MODE_PRIVATE)

    init
    {
        if(map.isEmpty())
        {
            lock.read {
                val mapString = spref.getString(SNOOZED_PREF, "")?.toByteArray(StandardCharsets.UTF_8) ?: "".toByteArray(StandardCharsets.UTF_8)
                try
                {
                    val bais = ByteArrayInputStream(Base64.decode(mapString, Base64.DEFAULT))
                    val inputStream = ObjectInputStream(bais)
                    map = inputStream.readObject() as MutableMap<Int, LocalTime>
                }
                catch (e : Exception)
                {
                    e.printStackTrace()
                }
            }
        }
    }

    fun isSnoozed(id : Int) : Boolean
    {
        lock.read {
            return map[id] != null && (map[id]?.isAfter(LocalTime.now()) ?: false)
        }
    }

    fun snoozeFor(id : Int, minutes : Int)
    {
        lock.write {
            Log.d("SNOOZE", "Ritardo il task $id per $minutes minuti")
            map.put(id, LocalTime.now().plusMinutes(minutes))
            val baos = ByteArrayOutputStream()
            val oos = ObjectOutputStream(baos)
            oos.writeObject(map)
            oos.flush()
            with(spref.edit())
            {
                putString(SNOOZED_PREF,Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT))
                apply()
            }

            val curSnooze = stat.getString(id.toString(), 0.toString())?.toInt() ?: 0

            with(stat.edit())
            {
                putString(id.toString(), (curSnooze+1).toString())
                apply()
            }
        }
    }

    fun unSnooze(id : Int)
    {
        lock.write {
            Log.d("SNOOZE", "Rimuovo il task $id")
            map.remove(id)
            val baos = ByteArrayOutputStream()
            val oos = ObjectOutputStream(baos)
            oos.writeObject(map)
            oos.flush()
            with(spref.edit())
            {
                putString(SNOOZED_PREF,Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT))
                apply()
            }
        }
    }
}