package ap.mnemosyne.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity;
import android.view.View
import ap.mnemosyne.resources.Task
import ap.mnemosyne.resources.TaskPlaceConstraint
import ap.mnemosyne.resources.TaskTimeConstraint
import apontini.mnemosyne.R

import kotlinx.android.synthetic.main.activity_task_details.*
import kotlinx.android.synthetic.main.content_task_details.*

class TaskDetailsActivity : AppCompatActivity()
{

    override fun onCreate(savedInstanceState: Bundle?)
    {

        val task: Task = intent.extras.getSerializable("task") as Task

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_details)

        toolbar.title = task.name.capitalize()
        setSupportActionBar(toolbar)

        if (!task.placesToSatisfy.isEmpty())
        {
            viewPlacesButton.setOnClickListener{
                val intent = Intent(this, MapsActivity::class.java)
                intent.putExtra("places", task.placesToSatisfy as HashSet)
                startActivity(intent)
            }
        }
        else
        {
            viewPlacesButton.visibility = View.GONE
        }

        if (task.constr is TaskPlaceConstraint)
        {
            textNameP1.text = getString(R.string.text_details_temporalConstr)
            textValueP1.text = task.constr.type.toString()
            textNameP2.text = getString(R.string.text_details_normact)
            textNameP2.text = (task.constr as TaskPlaceConstraint).normalizedAction.toString()
            textNameP3.text = getString(R.string.text_details_constrPlace)
            textNameP3.text = (task.constr as TaskPlaceConstraint).constraintPlace?.placeType?.toString() ?: "-"
            textNameP4.text = getString(R.string.text_details_paramName)
            textNameP4.text = (task.constr as TaskPlaceConstraint).paramName.toString()
        }
        else if (task.constr is TaskTimeConstraint)
        {
            textNameP1.text = getString(R.string.text_details_temporalConstr)
            textValueP1.text = task.constr.type.toString()
            textNameP2.text = getString(R.string.text_details_fromtime)
            textValueP2.text = (task.constr as TaskTimeConstraint).fromTime?.toString("HH:mm") ?: "-"
            textNameP3.text = getString(R.string.tedt_details_toTime)
            textValueP3.text = (task.constr as TaskTimeConstraint).toTime?.toString("HH:mm") ?: "-"
            textNameP4.text = getString(R.string.text_details_paramName)
            textValueP4.text = task.constr.paramName.toString()
        }
        else
        {
            textNameP1.text = getString(R.string.text_details_noConstraint)
            textValueP1.visibility = View.GONE
            textNameP2.visibility = View.GONE
            textValueP2.visibility = View.GONE
            textNameP3.visibility = View.GONE
            textValueP3.visibility = View.GONE
            textNameP4.visibility = View.GONE
            textValueP4.visibility = View.GONE
        }

        textPossibleWorkValue.text = if (task.isPossibleAtWork)
        {
            getString(R.string.text_yes)
        }
        else
        {
            getString(R.string.text_no)
        }
        textDoneTodayValue.text = if (task.isDoneToday)
        {
            getString(R.string.text_yes)
        }
        else
        {
            getString(R.string.text_no)
        }
        textRepeatableValue.text = if (task.isRepeatable)
        {
            getString(R.string.text_yes)
        }
        else
        {
            getString(R.string.text_no)
        }
        textFailedValue.text = if (task.isFailed)
        {
            getString(R.string.text_yes)
        }
        else
        {
            getString(R.string.text_no)
        }

    }
}
