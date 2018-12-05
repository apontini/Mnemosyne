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
import kotlinx.android.synthetic.main.card_number_layout.view.*

class MainCardsAdapter(private val context: Context,
                       private val data: List<Card>) : RecyclerView.Adapter<MainCardsAdapter.ViewHolder>()
{
    override fun onBindViewHolder(holder: MainCardsAdapter.ViewHolder, position: Int)
    {
        holder?.setItem(data[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.card_number_layout, parent, false), context)
    }

    override fun getItemCount(): Int
    {
        return data.size
    }

    class ViewHolder(view: View, ctx: Context) : RecyclerView.ViewHolder(view)
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
}