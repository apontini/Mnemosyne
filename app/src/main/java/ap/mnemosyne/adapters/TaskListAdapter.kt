package ap.mnemosyne.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import ap.mnemosyne.activities.TaskDetailsActivity
import ap.mnemosyne.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.task_list_item.view.*
import androidx.core.app.ActivityOptionsCompat
import ap.mnemosyne.resources.Place
import ap.mnemosyne.resources.Task
import com.google.android.gms.maps.model.MarkerOptions


class TaskListAdapter(private val context: Context,
                      private val data: List<Task>) : RecyclerView.Adapter<TaskListAdapter.ViewHolder>()
{

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.task_list_item, parent, false), context)
    }

    override fun getItemCount(): Int
    {
        return data.size
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int)
    {
        holder?.setItem(data[position])
    }

    class ViewHolder (view: View, ctx: Context) : RecyclerView.ViewHolder(view), View.OnClickListener
    {
        val map = view.map
        val nametext = view.textTaskName as TextView
        val extratext = view.textTaskExtra as TextView
        lateinit var task : Task
        val ctx : Context = ctx
        val v : View = view

        init
        {
            view.setOnClickListener(this)
        }

        fun setItem(t: Task)
        {
            task = t
            val options = GoogleMapOptions().liteMode(true)

            map.onCreate(Bundle())
            map.isClickable = false

            map.getMapAsync {
                it.uiSettings.isMapToolbarEnabled = false
                val list = t.placesToSatisfy as HashSet<Place>
                val map = it
                if(!list.isEmpty())
                {
                    list.forEach {
                        val latlon = LatLng(it.coordinates.lat, it.coordinates.lon)
                        map.addMarker(MarkerOptions().position(latlon).title(it.name).title(it.name ?: "Nome non trovato"))
                    }
                    val camera = LatLng(list.first().coordinates.lat, list.first().coordinates.lon)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(camera, 13.0f))
                }
            }

            nametext.text = task.name.capitalize()
            extratext.text = "Fallito: ${if(task.isFailed) ctx.getString(R.string.text_yes) else ctx.getString(R.string.text_no)}"
        }

        override fun onClick(p0: View?)
        {
            val detailIntent = Intent(p0?.context, TaskDetailsActivity::class.java)
            detailIntent.putExtra("task", task)
            detailIntent.putExtra("focusPlace", task.placesToSatisfy.first())
            if(ctx is Activity)
            {
                val p1 : androidx.core.util.Pair<View, String> = androidx.core.util.Pair(v.findViewById(R.id.map),"mapView")
                val p2 : androidx.core.util.Pair<View, String> = androidx.core.util.Pair(v.findViewById(R.id.textTaskName),"taskName")

                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(ctx, p1, p2)
                ctx.startActivityForResult(detailIntent, 101, options.toBundle())
            }
            else
            {
                ctx.startActivity(detailIntent)
            }
        }
    }
}