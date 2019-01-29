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
import ap.mnemosyne.http.HttpHelper
import ap.mnemosyne.permissions.PermissionsHelper
import ap.mnemosyne.resources.Task
import ap.mnemosyne.session.SessionHelper
import ap.mnemosyne.tasks.TasksHelper
import kotlinx.android.synthetic.main.activity_task_list.*
import kotlinx.android.synthetic.main.drawer_header.view.*
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
        setNavDrawer()

        noConnIcon.setOnClickListener {
            loadTasks(false)
        }

        layout_listTask.setOnRefreshListener {
            loadTasks(true)
        }

        loadTasks(false)


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
        noConnIcon.visibility = View.GONE
        tasks.updateTasksAndDo(forceRefresh, doWhat = {
            if(!layout_listTask.isRefreshing)
            {
                layout_listTask.visibility = View.GONE
                progressList.visibility = View.VISIBLE
                textProgressList.visibility = View.VISIBLE
                emptyListFrame.visibility = View.GONE
            }

            taskJSONList.clear()
            (tasks.getLocalTasks() as Map<Int, Task>).entries.forEach {
                taskJSONList.add(it.value)
            }
            if(!taskJSONList.isEmpty())
            {
                layout_listTask.isRefreshing = false
                taskList.layoutManager = LinearLayoutManager(this@TaskListActivity)
                taskList.layoutAnimation =
                        AnimationUtils.loadLayoutAnimation(this@TaskListActivity, R.anim.slide_from_bottom_animator)
                taskList.adapter = TaskListAdapter(this@TaskListActivity, taskJSONList)
                textProgressList.visibility = View.GONE
                layout_listTask.visibility = View.VISIBLE
                progressList.visibility = View.GONE
            }
            else
            {
                progressList.visibility = View.GONE
                textProgressList.visibility = View.GONE
                emptyListFrame.visibility = View.VISIBLE
            }
        },
            doWhatError = { p0, p1 ->
                if(p0 == HttpHelper.ERROR_NO_CONNECTION)
                {
                    layout_listTask.visibility = View.GONE
                    progressList.visibility = View.GONE
                    textProgressList.visibility = View.GONE
                    noConnIcon.visibility = View.VISIBLE
                }
                else if(p0 == HttpHelper.ERROR_PERMISSIONS)
                {
                    PermissionsHelper.askInternetPermission(this)
                }
            }
        )
    }

    private fun setNavDrawer()
    {
        nav_view.getHeaderView(0).header_text.text = session.user.email
        nav_view.setNavigationItemSelectedListener { menuItem ->

            when(menuItem.itemId)
            {
                R.id.create_task_voice ->
                {
                    val intent = Intent(this, VoiceActivity::class.java)
                    intent.putExtra("sessionid", session.user.sessionID)
                    startActivityForResult(intent, 1)
                }

                R.id.create_task_manual ->
                {
                    toolbar.snackbar(getString(R.string.error_notImplemented)).show()
                }
            }
            drawer_layout.closeDrawers()
            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        when(requestCode)
        {
            101 ->{
                if(resultCode == 1000 && data?.getSerializableExtra("deletedTask") != null )
                {
                    toolbar.longSnackbar(getString(R.string.info_removed)).show()
                    val tbr = data.getSerializableExtra("deletedTask") as Task
                    tasks.removeLocalTasks(tbr)
                    val pos = taskJSONList.indexOf(tbr)
                    taskJSONList.removeAt(pos)
                    taskList.adapter!!.notifyItemRemoved(pos)
                }
            }

            1->{
                when(resultCode)
                {
                    Activity.RESULT_OK -> {
                        loadTasks(false)
                        val intent = Intent(this, TaskDetailsActivity::class.java)
                        intent.putExtra("task", (data?.getSerializableExtra("resultTask") as Task).id)
                        startActivityForResult(intent, 101)
                    }
                }
            }

            SessionHelper.LOGIN_REQUEST_CODE ->
            {
                when(resultCode)
                {
                    Activity.RESULT_OK ->
                    {
                        val splashIntent = Intent(this, SplashActivity::class.java)
                        splashIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(splashIntent)
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
