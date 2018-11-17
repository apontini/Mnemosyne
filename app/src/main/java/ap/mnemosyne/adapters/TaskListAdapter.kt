package ap.mnemosyne.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import ap.mnemosyne.resources.Task
import apontini.mnemosyne.R

class TaskListAdapter(private val context: Context,
                      private val data: List<Task>) : BaseAdapter()
{

    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View
    {
        // Get view for row item
        val rowView = inflater.inflate(R.layout.task_list_item, p2, false)

        val idtext = rowView.findViewById(R.id.textTaskID) as TextView
        val nametext = rowView.findViewById(R.id.textTaskName) as TextView
        val extratext = rowView.findViewById(R.id.textTaskExtra) as TextView

        val curTask = getItem(p0) as Task

        idtext.text = curTask.id.toString()
        nametext.text = curTask.name.capitalize()
        extratext.text = "Failed: " + curTask.isFailed

        return rowView
    }

    override fun getItem(p0: Int): Any
    {
        return data[p0]
    }

    override fun getItemId(p0: Int): Long
    {
        return p0.toLong()
    }

    override fun getCount(): Int
    {
        return data.size
    }

}