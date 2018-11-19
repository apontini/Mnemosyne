package ap.mnemosyne.activities

import android.content.Context
import android.content.Intent
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
import kotlinx.android.synthetic.main.activity_task_details.*
import org.jetbrains.anko.alert



class TaskListActivity : AppCompatActivity()
{

    override fun onCreate(savedInstanceState: Bundle?)
    {
        val act = this
        val sharedPref = this.getSharedPreferences(getString(R.string.sharedPreferences_user_FILE), Context.MODE_PRIVATE)
        val sessionid = sharedPref.getString(getString(R.string.sharedPreferences_user_sessionid), "")
        val taskJSONList : MutableList<Task> = mutableListOf()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_list)
        setSupportActionBar(toolbar)

        doAsync {

            val request = Request.Builder()
                .addHeader("Cookie" , "JSESSIONID="+sessionid)
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
