package ap.mnemosyne.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.core.app.ActivityOptionsCompat
import ap.mnemosyne.http.HttpHelper
import ap.mnemosyne.resources.*
import ap.mnemosyne.session.SessionHelper
import ap.mnemosyne.R
import ap.mnemosyne.tasks.TasksHelper
import com.google.android.gms.maps.*

import kotlinx.android.synthetic.main.content_task_details.*
import okhttp3.Request
import org.jetbrains.anko.*
import org.jetbrains.anko.design.snackbar
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_task_details.*
import kotlinx.android.synthetic.main.drawer_header.view.*
import okhttp3.MediaType
import okhttp3.RequestBody
import org.jetbrains.anko.design.longSnackbar
import java.lang.Exception


class TaskDetailsActivity : AppCompatActivity(), OnMapReadyCallback
{
    private lateinit var session : SessionHelper
    private lateinit var task : Task
    private lateinit var tasks : TasksHelper
    private var focusPlace : Place? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        focusPlace = try {(intent.extras.getSerializable("focusPlace") as Place)} catch (e : Exception){null}
        tasks = TasksHelper(this)
        val id = intent.extras?.getInt("task") ?: -1
        task = tasks.getLocalTask(id) ?: Task(-1,"", "Task non trovato", null, false, false, false,
            false, false, HashSet<Place>())

        super.onCreate(savedInstanceState)
        session = SessionHelper(this)

        setContentView(R.layout.activity_task_details)
        setSupportActionBar(toolbar)
        setNavDrawer()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if(task.id == -1)
        {
            alert("Task non valido"){ okButton { supportFinishAfterTransition() }}.show()
        }

        if (!task.placesToSatisfy.isEmpty())
        {
            map.onCreate(savedInstanceState)
            val options = GoogleMapOptions().liteMode(true)
            map.getMapAsync(this)
            map.isClickable = false
            mapFrame.setOnClickListener {
                val intent = Intent(this, MapsActivity::class.java)
                val p1 : androidx.core.util.Pair<View, String> = androidx.core.util.Pair(map,"mapView")
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this@TaskDetailsActivity, p1)
                intent.putExtra("places", task.placesToSatisfy as HashSet<Place>)
                intent.putExtra("focusPlace", focusPlace)
                startActivity(intent, options.toBundle())
            }
        }
        else
        {
            map.visibility = View.GONE
        }

        checkPossibleWork.setOnClickListener {
            updateTask()
        }

        checkRepeatable.setOnClickListener {
            updateTask()
        }

        deleteTaskButton.setOnClickListener{

            alert("Sei sicuro?"){
                yesButton { deleteTask(task) }
                noButton { }
            }.show()
        }

        taskName.text = task.name.capitalize()

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

        var hidden = true
        detailsToggle.text = getString(R.string.text_details_showDetails)
        details1.visibility = View.GONE
        details2.visibility = View.GONE
        details3.visibility = View.GONE
        details4.visibility = View.GONE

        detailsToggle.setOnClickListener {
            if(hidden)
            {
                detailsToggle.text = getString(R.string.text_details_hideDetails)
                details1.visibility = View.VISIBLE
                details2.visibility = View.VISIBLE
                details3.visibility = View.VISIBLE
                details4.visibility = View.VISIBLE
            }
            else
            {
                detailsToggle.text = getString(R.string.text_details_showDetails)
                details1.visibility = View.GONE
                details2.visibility = View.GONE
                details3.visibility = View.GONE
                details4.visibility = View.GONE
            }
            hidden = !hidden
        }

        checkPossibleWork.isChecked = task.isPossibleAtWork

        textDoneTodayValue.text = if (task.isDoneToday)
        {
            getString(R.string.text_yes)
        }
        else
        {
            getString(R.string.text_no)
        }

        checkRepeatable.isChecked = task.isRepeatable

        textFailedValue.text = if (task.isFailed)
        {
            getString(R.string.text_yes)
        }
        else
        {
            getString(R.string.text_no)
        }
    }

    override fun onMapReady(p0: GoogleMap)
    {
        p0.uiSettings.isMapToolbarEnabled = false
        val list = task.placesToSatisfy as HashSet<Place>
        Log.d("LISTA", list.toString())
        if(!list.isEmpty())
        {
            list.forEach {
                val latlon = LatLng(it.coordinates.lat, it.coordinates.lon)
                p0.addMarker(MarkerOptions().position(latlon).title(it.name).title(it.name ?: "Nome non trovato"))
            }

            if(focusPlace != null)
            {
                val camera = LatLng((focusPlace as Place).coordinates.lat,
                    (focusPlace as Place).coordinates.lon)
                p0.moveCamera(CameraUpdateFactory.newLatLngZoom(camera, 13.0f))
            }
            else
            {
                if(!list.isEmpty())
                {
                    val camera = LatLng(
                        list.first().coordinates.lat,
                        list.first().coordinates.lon
                    )
                    p0.moveCamera(CameraUpdateFactory.newLatLngZoom(camera, 13.0f))
                }
            }
        }
        else
        {
            alert("Nessun posto indicato") {}.show()
            finish()
        }
    }

    private fun updateTask()
    {
        checkPossibleWork.isEnabled = false
        checkRepeatable.isEnabled = false
        progressBar3.visibility = View.VISIBLE

        val newTask = Task(task.id, task.user, task.name, task.constr, checkPossibleWork.isChecked,
            checkRepeatable.isChecked, task.isDoneToday, task.isFailed, task.isIgnoredToday, task.placesToSatisfy)

        val request = Request.Builder()
            .addHeader("Cookie" , "JSESSIONID="+session.user.sessionID)
            .url(HttpHelper.REST_TASK_URL)
            .put(RequestBody.create(MediaType.parse("application/json"), newTask.toJSON()))
            .build()

        doAsync {
            val resp = HttpHelper(this@TaskDetailsActivity).request(request, true)
            when(resp.second.code())
            {
                401 ->
                {
                    Log.d("SESSION", "Sessione scaduta")
                    val intent = Intent(this@TaskDetailsActivity, LoginActivity::class.java)
                    startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
                }

                200 ->
                {
                    toolbar.snackbar("Task aggiornato con successo")
                    tasks.modifyLocalTasks(newTask)
                }

                404 ->{
                    uiThread { alert("Errore durante l'update del task (404)") {  }.show() }
                }

                HttpHelper.ERROR_PERMISSIONS ->
                {
                    uiThread { alert(getString(R.string.alert_noInternetPermission)) {  }.show() }
                }

                HttpHelper.ERROR_NO_CONNECTION ->
                {
                    uiThread { alert(getString(R.string.alert_noInternetConnection)) {  }.show() }
                }

                else ->{
                    Log.d("MESSAGGIO", resp.second.code().toString())
                }
            }
            uiThread {
                checkPossibleWork.isEnabled = true
                checkRepeatable.isEnabled = true
                progressBar3.visibility = View.GONE
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        return when (item.itemId)
        {
            android.R.id.home -> {
                supportFinishAfterTransition()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun deleteTask(task : Task)
    {
        deleteTaskButton.isClickable = false
        progressBar3.visibility = View.VISIBLE
        val request = Request.Builder()
            .addHeader("Cookie" , "JSESSIONID="+session.user.sessionID)
            .url(HttpHelper.REST_TASK_URL + "/" + task.id)
            .delete()
            .build()
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

                HttpHelper.ERROR_NO_CONNECTION ->{
                    uiThread { alert(getString(R.string.alert_noInternetConnection)) {  }.show() }
                    error = true
                }

                HttpHelper.ERROR_UNKNOWN->{
                uiThread {
                    alert(getString(R.string.alert_generalError)) {  }.show()
                }
            }

                else ->{
                    Log.d("MESSAGGIO", resp.second.code().toString())
                    error = true
                }
            }
            if(error)
            {
                uiThread {
                    deleteTaskButton.isClickable = true
                    progressBar3.visibility = View.INVISIBLE
                    tableLayout.snackbar((resp.first as Message).errorDetails)
                }
            }
        }
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

                R.id.task_gallery ->
                {
                    val intent = Intent(this, TaskListActivity::class.java)
                    this.startActivityForResult(intent, 101)
                }

                R.id.create_task_manual ->
                {
                    toolbar.snackbar("Non implementato").show()
                }

            }
            drawer_layout.closeDrawers()
            true
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
                    toolbar.snackbar("Sei collegato come: " + session.user.email).show()
                }
                else
                {
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
                }
            }

            1->{
                when(resultCode)
                {
                    Activity.RESULT_OK -> {
                        val intent = Intent(this, TaskDetailsActivity::class.java)
                        intent.putExtra("task", (data?.getSerializableExtra("resultTask") as Task).id)
                        startActivityForResult(intent, 102)
                    }
                }
            }

            102->
            {
                if(resultCode == 1000 && data?.getSerializableExtra("deletedTask") != null )
                {
                    toolbar.longSnackbar("Rimosso").show()
                }
            }
        }
    }
}
