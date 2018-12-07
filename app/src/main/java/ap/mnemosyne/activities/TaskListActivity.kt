package ap.mnemosyne.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import ap.mnemosyne.R
import kotlinx.android.synthetic.main.content_task_list.*
import ap.mnemosyne.adapters.TaskListAdapter
import ap.mnemosyne.resources.Task
import ap.mnemosyne.session.SessionHelper
import ap.mnemosyne.tasks.TasksHelper
import kotlinx.android.synthetic.main.activity_task_list.*
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.design.snackbar


class TaskListActivity : AppCompatActivity()
{
    private lateinit var session : SessionHelper
    private val taskJSONList : MutableList<Task> = mutableListOf()
    private lateinit var tasks : TasksHelper

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        session = SessionHelper(this)
        tasks = TasksHelper(this)

        setContentView(R.layout.activity_task_list)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        session.checkSessionValidity{loadTasks(false)}

        layout_listTask.setOnRefreshListener {
            loadTasks(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        return when (item.itemId)
        {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRestart()
    {
        super.onRestart()
        session.checkSessionValidity{}
    }

    fun loadTasks(forceRefresh : Boolean)
    {
        tasks.updateTasksAndDo(forceRefresh) {
            if(!layout_listTask.isRefreshing)
            {
                taskList.visibility = View.GONE
                progressList.visibility = View.VISIBLE
                textProgressList.visibility = View.VISIBLE
                emptyListFrame.visibility = View.GONE
            }

            taskJSONList.clear()
            (tasks.getLocalTasks() as List<Task>).forEach {
                taskJSONList.add(it)
            }
            if(!taskJSONList.isEmpty())
            {
                layout_listTask.isRefreshing = false
                taskList.layoutManager = LinearLayoutManager(this@TaskListActivity)
                taskList.layoutAnimation =
                        AnimationUtils.loadLayoutAnimation(this@TaskListActivity, R.anim.slide_from_bottom_animator)
                taskList.adapter = TaskListAdapter(this@TaskListActivity, taskJSONList)
                textProgressList.visibility = View.GONE
                taskList.visibility = View.VISIBLE
                progressList.visibility = View.GONE
            }
            else
            {
                progressList.visibility = View.GONE
                textProgressList.visibility = View.GONE
                emptyListFrame.visibility = View.VISIBLE
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
                    toolbar.longSnackbar("Rimosso").show()
                    val tbr = data.getSerializableExtra("deletedTask") as Task
                    tasks.removeLocalTasks(tbr)
                    val pos = taskJSONList.indexOf(tbr)
                    taskJSONList.removeAt(pos)
                    taskList.adapter!!.notifyItemRemoved(pos)
                }
            }

            SessionHelper.LOGIN_REQUEST_CODE ->
            {
                when(resultCode)
                {
                    Activity.RESULT_OK ->
                    {
                        loadTasks(true)
                        toolbar.snackbar("Sei collegato come: " + session.user.email).show()
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
