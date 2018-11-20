package ap.mnemosyne.httphandler

import android.app.Activity
import android.os.Looper
import android.util.Log
import ap.mnemosyne.permissions.PermissionsHandler
import ap.mnemosyne.resources.Message
import ap.mnemosyne.resources.Resource
import com.fasterxml.jackson.core.JsonParseException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jetbrains.anko.alert
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.toast
import java.io.ByteArrayInputStream
import java.lang.ClassCastException
import java.util.concurrent.TimeUnit
import kotlin.math.log

class HttpHandler(act: Activity)
{
    companion object
    {
        val httpclient : OkHttpClient by lazy{
            val b = OkHttpClient.Builder()
            b.readTimeout(500, TimeUnit.SECONDS)
            b.writeTimeout(20, TimeUnit.SECONDS)
            return@lazy b.build()
        }
        const val AUTH_URL : String = "http://pintini.ddns.net:8080/mnemosyne/auth"
        const val REST_USER_URL : String = "http://pintini.ddns.net:8080/mnemosyne/rest/user"
        const val REST_TASK_URL : String = "http://pintini.ddns.net:8080/mnemosyne/rest/task"
        const val PARSE_URL : String = "http://pintini.ddns.net:8080/mnemosyne/parse"
    }

    val act : Activity

    init
    {
         this.act = act
    }


    fun request(req : Request, parseRes : Boolean) : Pair<Resource?, Response>
    {
        if(!PermissionsHandler.checkInternetPermission(act))
        {
            //ERROR: permission are not given
            val res = Response.Builder().code(999).build()
            return Pair(null, res)
        }

        synchronized(httpclient)
        {
            val resp = httpclient.newCall(req).execute()
            var resRet : Resource? = null
            if(parseRes)
            {
                val bodyResp: String = resp.body()?.string() ?: Message("Errore", "", "null dal server").toJSON()
                Log.d("RESPONSE", bodyResp)
                val stream = ByteArrayInputStream(bodyResp.toByteArray(Charsets.UTF_8))
                resRet = try{ Resource.fromJSON(stream) }
                    catch (jpe : JsonParseException) { Message("JPE", "", jpe.message) }
                    catch (cce : ClassCastException){ Message("CCE", "", cce.message) }

            }
            return Pair(resRet, resp)
        }
    }

    fun request(req: Request) : Response
    {
        return request(req, false).second
    }
}
