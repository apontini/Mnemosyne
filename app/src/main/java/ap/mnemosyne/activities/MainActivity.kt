package ap.mnemosyne.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import ap.mnemosyne.permissions.PermissionsHelper
import ap.mnemosyne.session.SessionHelper
import ap.mnemosyne.R

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.design.snackbar
import android.view.animation.AnimationUtils
import ap.mnemosyne.adapters.MainCardsAdapter
import ap.mnemosyne.uiResources.Card
import ap.mnemosyne.uiResources.NumberCard
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.core.view.GravityCompat
import ap.mnemosyne.tasks.TasksHelper
import ap.mnemosyne.R.id.*
import ap.mnemosyne.resources.*
import ap.mnemosyne.uiResources.TaskCard
import kotlinx.android.synthetic.main.drawer_header.view.*
import org.jetbrains.anko.design.longSnackbar
import org.joda.time.LocalDateTime
import org.joda.time.Minutes
import java.lang.Exception

class MainActivity : AppCompatActivity()
{

    private lateinit var session : SessionHelper
    private lateinit var tasks : TasksHelper
    private var isViewCreated = false

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        Log.d("LIFECYCLE", "onCreate")
        session = SessionHelper(this)
        tasks = TasksHelper(this)
        session.checkSessionValidity {
            isViewCreated = true
            createContentView() }
        PermissionsHelper.askPermissions(this)
    }

    private fun createContentView()
    {
        setContentView(R.layout.activity_main)
        setToolbar()
        setNavDrawer()
        setCards(true)

        toolbar.snackbar("Sei collegato come: " + session.user.email).show()

        noConnIcon.setOnClickListener {
            setCards(true)
        }

        fab.setOnClickListener {
            val intent = Intent(this, VoiceActivity::class.java)
            intent.putExtra("sessionid", session.user.sessionID)
            startActivityForResult(intent, 1)
        }
    }

    override fun onPause() {
        super.onPause()
        if(mainProgress != null) mainProgress.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        if(mainProgress != null) mainProgress.visibility = View.GONE
    }

    private fun setCards(animate : Boolean)
    {
        noConnIcon.visibility = View.GONE
        fab.isEnabled = true
        mainProgress.visibility = View.VISIBLE
        mainText.visibility = View.VISIBLE
        cardList.visibility = View.GONE
        tasks.updateTasksAndDo(doWhat = {
            val tasksList : List<Task> = tasks.getLocalTasks() as List<Task>? ?: listOf()
            var constrained = 0
            var failed = 0
            var doneToday = 0
            var timeConstr = 0
            var placeConstr = 0

            tasksList.forEach{
                if(it.isFailed) failed++
                if(it.isDoneToday) doneToday++
                if(it.constr != null)
                {
                    constrained++
                    if(it.constr is TaskPlaceConstraint) placeConstr++
                    else if (it.constr is TaskTimeConstraint) timeConstr++
                }
            }

            val cardCreatedList = mutableListOf<Card>(NumberCard(tasksList.size, "Task registrati"), NumberCard(doneToday, "Task completati oggi"),
                NumberCard(failed, "Task falliti"))

            val prefs = getSharedPreferences(getString(R.string.sharedPreferences_tasks_FILE), Context.MODE_PRIVATE)
            val lastRefr = LocalDateTime.parse(prefs.getString(getString(R.string.sharedPreferences_tasks_hints_lastRefresh), "1970-01-01 00:00"), TasksHelper.dateTimeFormat)
            val lastUser = prefs.getString(getString(R.string.sharedPreferences_tasks_hints_user), "")

            if(Minutes.minutesBetween(lastRefr, LocalDateTime.now()).minutes <= 15 && session.user.email == lastUser)
            {
                try
                {
                    val hints = ResourceList.fromJSON(
                        prefs.getString(getString(R.string.sharedPreferences_tasks_hints), "")
                    ).list as List<Hint>

                    val defaultTask = Task(-1,"", "Task non trovato", null, false, false, false, false,
                        false, false, HashSet<Place>())

                    hints.filter { it.isUrgent && !it.isConfirm }.forEach {
                        cardCreatedList.add(TaskCard(
                            tasks.getLocalTask(it.taskID) ?: defaultTask, it))
                    }

                    hints.filter { !it.isUrgent }.forEachIndexed{ i, e -> if(i<2) cardCreatedList.add(TaskCard(tasks.getLocalTask(e.taskID) ?: defaultTask, e));}
                }
                catch(e : Exception)
                {
                    e.printStackTrace()
                }
            }

            cardList.setHasFixedSize(true)
            cardList.layoutManager = StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL)
            if(animate) cardList.layoutAnimation = AnimationUtils.loadLayoutAnimation(this, R.anim.slide_from_bottom_animator)
            else cardList.layoutAnimation = null
            if(cardList.itemDecorationCount == 0) cardList.addItemDecoration(SpaceItemDecoration(-8)) //margin is 8dp
            cardList.adapter = MainCardsAdapter(this@MainActivity, cardCreatedList)

            mainProgress.visibility = View.GONE
            mainText.visibility = View.GONE
            cardList.visibility = View.VISIBLE
        },
            doWhatError = { p0,p1 ->
                fab.isEnabled = false
                noConnIcon.visibility = View.VISIBLE
                mainProgress.visibility = View.GONE
                mainText.visibility = View.GONE
            })
    }

    private fun setToolbar()
    {
        setSupportActionBar(toolbar)
        supportActionBar?.apply{
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_baseline_menu_24px)
        }
    }

    private fun setNavDrawer()
    {
        nav_view.getHeaderView(0).header_text.text = session.user.email
        nav_view.setNavigationItemSelectedListener { menuItem ->

            when(menuItem.itemId)
            {
                create_task_voice ->
                {
                    val intent = Intent(this, VoiceActivity::class.java)
                    intent.putExtra("sessionid", session.user.sessionID)
                    startActivityForResult(intent, 1)
                }

                task_gallery ->
                {
                    val intent = Intent(this, TaskListActivity::class.java)
                    this.startActivityForResult(intent, 101)
                }

                create_task_manual ->
                {
                    toolbar.snackbar("Non implementato").show()
                }

                action_settings ->
                {
                    val animation = AnimationUtils.loadAnimation(this, R.anim.spin_around_itself)
                    findViewById<View>(R.id.action_settings).startAnimation(animation)
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivityForResult(intent, 2)
                }

            }
            drawer_layout.closeDrawers()
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        return when (item.itemId)
        {
            R.id.action_settings -> {
                val animation = AnimationUtils.loadAnimation(this, R.anim.spin_around_itself)
                findViewById<View>(R.id.action_settings).startAnimation(animation)
                val intent = Intent(this, SettingsActivity::class.java)
                startActivityForResult(intent, 2)
                true
            }
            android.R.id.home -> {
                drawer_layout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        if(mainProgress != null) mainProgress.visibility = View.GONE
        when(requestCode)
        {
            SessionHelper.LOGIN_REQUEST_CODE->{
                when(resultCode)
                {
                    Activity.RESULT_OK ->
                    {
                        isViewCreated = false
                        val splashIntent = Intent(this, SplashActivity::class.java)
                        splashIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(splashIntent)
                        /*if(!isViewCreated)
                        {
                            createContentView()
                        }
                        else
                        {
                            setCards(true)
                            toolbar.snackbar("Sei collegato come: " + session.user.email).show()
                            isViewCreated = false
                        }*/
                    }

                    else -> {
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
                    }
                }
            }

            1->{
               when(resultCode)
               {
                   Activity.RESULT_OK -> {
                       setCards(false)
                       val intent = Intent(this, TaskDetailsActivity::class.java)
                       intent.putExtra("task", (data?.getSerializableExtra("resultTask") as Task).id)
                       startActivityForResult(intent, 102)
                   }
               }
            }

            2->{
               when(resultCode)
               {
                   SettingsActivity.LOGOUT_RESULT_CODE -> {
                       val intent = Intent(this, LoginActivity::class.java)
                       startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
                       isViewCreated = true
                   }
               }
            }

            101->
            {
                setCards(false)
            }

            102->
            {
                setCards(false)
                if(resultCode == 1000 && data?.getSerializableExtra("deletedTask") != null )
                {
                     toolbar.longSnackbar("Rimosso").show()
                }
            }
        }
    }

    inner class SpaceItemDecoration(private val space: Int) : RecyclerView.ItemDecoration()
    {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State)
        {
            val position = parent.getChildAdapterPosition(view)

            val lp = view.layoutParams as StaggeredGridLayoutManager.LayoutParams
            val spanIndex = lp.spanIndex

            if (position >= 0)
            {
                if (spanIndex == 1)
                {
                    outRect.left = space
                }
                else
                {
                    outRect.right = space
                }
            }
        }
    }
}
