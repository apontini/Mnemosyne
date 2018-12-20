package ap.mnemosyne.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ap.mnemosyne.R


class OnboardingListAdapter(private val context: Context,
                      private val data: List<Pair<String, String>>) : RecyclerView.Adapter<OnboardingListAdapter.ViewHolder>()
{
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingListAdapter.ViewHolder
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
        //val text = view.textTaskExtra as TextView
        //val text2 = view.textTaskExtra2 as TextView
        val ctx : Context = ctx
        val v : View = view

        init
        {
            view.setOnClickListener(this)
        }

        fun setItem(p: Pair<String, String>)
        {

        }

        override fun onClick(p0: View?)
        {

        }
    }
}