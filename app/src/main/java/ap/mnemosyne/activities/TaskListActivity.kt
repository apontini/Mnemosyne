package ap.mnemosyne.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import ap.mnemosyne.httphandler.HttpHandler
import ap.mnemosyne.resources.Resource
import ap.mnemosyne.resources.ResourceList
import ap.mnemosyne.resources.Task
import apontini.mnemosyne.R
import kotlinx.android.synthetic.main.content_task_list.*
import okhttp3.Request
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.lang.Exception
import ap.mnemosyne.adapters.TaskListAdapter
import ap.mnemosyne.session.SessionManager
import kotlinx.android.synthetic.main.activity_task_details.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.okButton


class TaskListActivity : AppCompatActivity()
{


    private lateinit var session : SessionManager

    override fun onCreate(savedInstanceState: Bundle?)
    {
        val act = this
        val taskJSONList : MutableList<Task> = mutableListOf()

        super.onCreate(savedInstanceState)

        session = SessionManager(this)

        if(session.user.sessionID == null || session.user.sessionID == "")
        {
            alert(getString(R.string.alert_noSession)){
                okButton { finish() }
            }.show()
            return
        }

        setContentView(R.layout.activity_task_list)
        setSupportActionBar(toolbar)

        doAsync {

            val request = Request.Builder()
                .addHeader("Cookie" , "JSESSIONID="+session.user.sessionID)
                .url(HttpHandler.REST_TASK_URL)
                .build()

            var resultText : String = ""

            val res = HttpHandler(act).request(request)
            when(res.code())
            {
                200 ->{
                    if(res.header("Content-Type", "") != "")
                    {
                        var bodyResp : String = res.body()?.string() ?: ""

                        val resList = try {ResourceList.fromJSON(bodyResp)}
                                catch (jpe : Exception) {
                                    uiThread { alert(jpe.message.toString()){}.show() }
                                    jpe.printStackTrace()
                                    mutableListOf<Resource>()
                                }

                        resList.forEach {
                            taskJSONList.add(it as Task)
                        }
                    }
                    else
                    {
                        resultText = getString(R.string.text_list_noJSON)
                    }
                }

                401 -> resultText = getString(R.string.text_list_401)

                999 ->{
                    alert(getString(R.string.alert_noInternetPermission)) {  }.show()
                }

                else ->
                {
                    resultText = res.code().toString()
                }

            }

            uiThread {
                taskList.layoutManager = LinearLayoutManager(act)
                taskList.adapter = TaskListAdapter(act, taskJSONList)
                when(resultText)
                {
                    "" -> textProgressList.visibility = View.GONE
                    else -> textProgressList.text = resultText

                }
                progressList.visibility = View.GONE
            }
        }
    }
}
