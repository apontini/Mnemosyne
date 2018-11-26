package ap.mnemosyne.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.support.v7.app.AppCompatActivity;
import android.util.Log
import android.view.View
import ap.mnemosyne.http.HttpHelper
import ap.mnemosyne.resources.*
import ap.mnemosyne.session.SessionHelper
import apontini.mnemosyne.R

import kotlinx.android.synthetic.main.activity_task_details.*
import kotlinx.android.synthetic.main.content_task_details.*
import kotlinx.android.synthetic.main.content_voice.*
import okhttp3.Request
import org.jetbrains.anko.*
import org.jetbrains.anko.design.snackbar

class TaskDetailsActivity : AppCompatActivity()
{

    private lateinit var session : SessionHelper

    override fun onCreate(savedInstanceState: Bundle?)
    {

        val task: Task = intent.extras.getSerializable("task") as Task

        super.onCreate(savedInstanceState)
        session = SessionHelper(this)

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

        deleteTaskButton.setOnClickListener{

            alert("Sei sicuro?"){
                yesButton { deleteTask(task) }
                noButton { }
            }.show()
        }

        when
        {
            task.constr is TaskPlaceConstraint ->
            {
                textNameP1.text = getString(R.string.text_details_temporalConstr)
                textValueP1.text = task.constr.type.toString()
                textNameP2.text = getString(R.string.text_details_normact)
                textValueP2.text = (task.constr as TaskPlaceConstraint).normalizedAction.toString()
                textNameP3.text = getString(R.string.text_details_constrPlace)
                textValueP3.text = (task.constr as TaskPlaceConstraint).constraintPlace?.placeType?.toString() ?: "-"
                textNameP4.text = getString(R.string.text_details_paramName)
                textValueP4.text = (task.constr as TaskPlaceConstraint).paramName.toString()
            }
            task.constr is TaskTimeConstraint  ->
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
            else                               ->
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

    fun deleteTask(task : Task)
    {
        viewPlacesButton.isClickable = false
        deleteTaskButton.isClickable = false
        progressBar3.visibility = View.VISIBLE
        val request = Request.Builder()
            .addHeader("Cookie" , "JSESSIONID="+session.user.sessionID)
            .url(HttpHelper.REST_TASK_URL + "/" + task.id)
            .delete()
            .build()
        Log.d("URL", request.toString())
        var error = false
        doAsync {
            val resp = HttpHelper(this@TaskDetailsActivity).request(request, true)
            when(resp.second.code())
            {
                401 -> {
                    Log.d("SESSION", "Sessione scaduta")
                    error = true
                    val intent = Intent(this@TaskDetailsActivity, LoginActivity::class.java)
                    startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
                }

                200 -> {
                    val returnIntent = Intent()
                    returnIntent.putExtra("deletedTask", task)
                    setResult(1000, returnIntent)
                    finish()
                }

                HttpHelper.ERROR_PERMISSIONS ->{
                    uiThread { alert(getString(R.string.alert_noInternetPermission)) {  }.show() }
                    error = true
                }

                else ->{
                    Log.d("MESSAGGIO", resp.second.code().toString())
                    error = true
                }
            }
            if(error)
            {
                uiThread {
                    viewPlacesButton.isClickable = true
                    deleteTaskButton.isClickable = true
                    progressBar3.visibility = View.INVISIBLE
                    snackbar(tableLayout, (resp.first as Message).errorDetails)
                }
            }
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode)
        {
            SessionHelper.LOGIN_REQUEST_CODE ->
            {
                if (resultCode == Activity.RESULT_OK)
                {
                    snackbar(findViewById(R.id.layout_main), "Sei collegato come: " + session.user.email).show()
                    textStatus.text = getString(R.string.text_voice_retry)
                }
                else
                {
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
                }
            }
        }
    }
}
