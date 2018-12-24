package ap.mnemosyne.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ap.mnemosyne.activities.TaskListActivity
import ap.mnemosyne.uiResources.Card
import ap.mnemosyne.uiResources.NumberCard
import ap.mnemosyne.R
import ap.mnemosyne.activities.TaskDetailsActivity
import ap.mnemosyne.uiResources.TaskCard
import kotlinx.android.synthetic.main.card_number_layout.view.*
import kotlinx.android.synthetic.main.card_task_layout.view.*

class MainCardsAdapter(private val context: Context,
                       private val data: List<Card>) : RecyclerView.Adapter<RecyclerView.ViewHolder>()
{
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int)
    {
        when(holder.itemViewType)
        {
            0 -> (holder as ViewHolderNumber).setItem(data[position])
            1,2 -> (holder as ViewHolderTask).setItem(data[position])
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder
    {
        return when(viewType)
        {
            0 -> ViewHolderNumber(LayoutInflater.from(context).inflate(R.layout.card_number_layout, parent, false), context)
            1 -> ViewHolderTask(LayoutInflater.from(context).inflate(R.layout.card_task_layout, parent, false), context)
            2 -> ViewHolderTask(LayoutInflater.from(context).inflate(R.layout.card_task_layout_urgent, parent, false), context)
            else -> ViewHolderNumber(LayoutInflater.from(context).inflate(R.layout.card_number_layout, parent, false), context)
        }
    }

    override fun getItemViewType(position: Int): Int
    {
        return if(data[position] is NumberCard) 0
        else
        {
            if((data[position] as TaskCard).hint?.isUrgent == true) 2 else 1
        }
    }

    override fun getItemCount(): Int
    {
        return data.size
    }

    class ViewHolderNumber(view: View, ctx: Context) : RecyclerView.ViewHolder(view)
    {
        val title = view.title as TextView
        val descr = view.descr as TextView
        lateinit var card: Card
        val ctx: Context = ctx
        val v: View = view

        init
        {
            view.setOnClickListener{
                val detailIntent = Intent(it?.context, TaskListActivity::class.java)
                if (ctx is Activity)
                    ctx.startActivityForResult(detailIntent, 101)
                else
                    ctx.startActivity(detailIntent)
            }
        }

        fun setItem(c: Card)
        {
            card = c
            if(c is NumberCard)
            {
                title.text = c.title.toString()
                descr.text = c.desc
            }
        }
    }

    class ViewHolderTask(view: View, ctx: Context) : RecyclerView.ViewHolder(view)
    {
        val title = view.titleTask as TextView
        val descr = view.descrTask as TextView
        val road = view.roadTask as TextView
        lateinit var card: Card
        val ctx: Context = ctx
        val v: View = view

        fun setItem(c: Card)
        {
            card = c
            if(c is TaskCard)
            {
                v.setOnClickListener{
                    val detailIntent = Intent(it?.context, TaskDetailsActivity::class.java)
                    detailIntent.putExtra("task", c.task.id)
                    detailIntent.putExtra("focusPlace", c.hint?.closestPlace)
                    if (ctx is Activity)
                        ctx.startActivityForResult(detailIntent, 102)
                    else
                        ctx.startActivity(detailIntent)
                }
                title.text = c.task.name.capitalize()
                road.text = "${c.hint?.closestPlace?.road ?: ""}"

                if(road.text == "") road.visibility = View.GONE

                if(c.hint?.isUrgent == true)
                {
                    descr.text = "URGENTE!"
                }
                else
                {
                    descr.text = "Come prossima cosa.."
                }
            }
        }
    }
}