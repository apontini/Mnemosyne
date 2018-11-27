package ap.mnemosyne.activities

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import ap.mnemosyne.http.HttpHelper
import ap.mnemosyne.resources.ResourceList
import ap.mnemosyne.resources.Task
import apontini.mnemosyne.R
import kotlinx.android.synthetic.main.content_task_list.*
import okhttp3.Request
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import ap.mnemosyne.adapters.TaskListAdapter
import ap.mnemosyne.resources.Message
import ap.mnemosyne.session.SessionHelper
import kotlinx.android.synthetic.main.activity_task_details.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.design.snackbar


class TaskListActivity : AppCompatActivity()
{
    private lateinit var session : SessionHelper
    private val taskJSONList : MutableList<Task> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        session = SessionHelper(this)

        setContentView(R.layout.activity_task_list)
        setSupportActionBar(toolbar)

        session.checkSessionValidity{loadTasks()}

        layout_listTask.setOnRefreshListener {
            loadTasks()
        }
    }

    override fun onRestart()
    {
        super.onRestart()
        session.checkSessionValidity{}
    }

    fun loadTasks()
    {
        Log.d("LOADING", "loading tasks")
        if(!layout_listTask.isRefreshing)
        {
            taskList.visibility = View.GONE
            progressList.visibility = View.VISIBLE
            textProgressList.visibility = View.VISIBLE
        }
        doAsync {

            taskJSONList.clear()
            val request = Request.Builder()
                .addHeader("Cookie" , "JSESSIONID="+session.user.sessionID)
                .url(HttpHelper.REST_TASK_URL)
                .build()

            var resultText : String = ""

            val res = HttpHelper(this@TaskListActivity).request(request, true)
            when(res.second.code())
            {
                200 ->{
                    if(res.second.header("Content-Type", "") != "")
                    {
                        val resList = res.first as ResourceList<Task>

                        resList.list.forEach {
                            taskJSONList.add(it as Task)
                        }
                    }
                    else
                    {
                        resultText = getString(R.string.text_list_noJSON)
                    }
                }

                401 -> resultText = getString(R.string.text_list_401)

                HttpHelper.ERROR_PERMISSIONS ->{
                    uiThread { alert(getString(R.string.alert_noInternetPermission)) {  }.show() }
                }

                else ->
                {
                    resultText = (res.first as Message).errorDetails
                }

            }

            uiThread {
                layout_listTask.isRefreshing = false
                taskList.layoutManager = LinearLayoutManager(this@TaskListActivity)
                taskList.adapter = TaskListAdapter(this@TaskListActivity, taskJSONList)
                when(resultText)
                {
                    "" ->
                    {
                        textProgressList.visibility = View.GONE
                        taskList.visibility = View.VISIBLE
                    }
                    else -> textProgressList.text = resultText

                }
                progressList.visibility = View.GONE
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        when(requestCode)
        {
            101 ->{
                if(resultCode == 1000 && data?.getSerializableExtra("deletedTask") != null )
                {
                    val pos = taskJSONList.indexOf(data.getSerializableExtra("deletedTask") as Task)
                    taskJSONList.removeAt(pos)
                    taskList.adapter.notifyItemRemoved(pos)
                }
            }

            SessionHelper.LOGIN_REQUEST_CODE ->
            {
                when(resultCode)
                {
                    Activity.RESULT_OK ->
                    {
                        loadTasks()
                        snackbar(toolbar, "Sei collegato come: " + session.user.email).show()
                    }

                    else -> {
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
                    }
                }
            }
        }
    }
}
