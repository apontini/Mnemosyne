package ap.mnemosyne.http

import android.app.Activity
import android.util.Log
import ap.mnemosyne.permissions.PermissionsHelper
import ap.mnemosyne.resources.Message
import ap.mnemosyne.resources.Resource
import ap.mnemosyne.resources.ResourceList
import com.fasterxml.jackson.core.JsonParseException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayInputStream
import java.lang.ClassCastException
import java.util.concurrent.TimeUnit

class HttpHelper(act: Activity)
{
    companion object
    {
        val httpclient : OkHttpClient by lazy{
            val b = OkHttpClient.Builder()
            b.readTimeout(500, TimeUnit.SECONDS)
            b.writeTimeout(20, TimeUnit.SECONDS)
            return@lazy b.build()
        }

        const val BASE_URL : String = "http://pintini.ddns.net:8080"
        const val AUTH_URL : String = "$BASE_URL/mnemosyne/auth"
        const val REST_USER_URL : String = "$BASE_URL/mnemosyne/rest/user"
        const val REST_TASK_URL : String = "$BASE_URL/mnemosyne/rest/task"
        const val PARSE_URL : String = "$BASE_URL/mnemosyne/parse"
        const val REST_PARAMETER_URL : String = "$BASE_URL/mnemosyne/rest/parameter"
    }

    val act : Activity = act

    fun request(req : Request, parseRes : Boolean) : Pair<Resource?, Response>
    {
        if(!PermissionsHelper.checkInternetPermission(act))
        {
            //ERROR: permission are not given
            val res = Response.Builder().code(999).build()
            return Pair(null, res)
        }

        synchronized(httpclient)
        {
            val resp = httpclient.newCall(req).execute()
            lateinit var resRet : Resource
            if(parseRes)
            {
                val bodyResp: String = resp.body()?.string() ?: Message("Errore", "", "null dal server").toJSON()
                Log.d("RESPONSE", bodyResp)
                if(bodyResp.contains("{\"resource-list\":["))
                {
                    resRet = try
                    {
                        ResourceList.fromJSON(bodyResp)
                    }
                    catch (jpe: JsonParseException)
                    {
                        Message("JPE", "", jpe.message)
                    }
                    catch (cce: ClassCastException)
                    {
                        Message("CCE", "", cce.message)
                    }
                }
                else
                {
                    val stream = ByteArrayInputStream(bodyResp.toByteArray(Charsets.UTF_8))
                    resRet = try
                    {
                        Resource.fromJSON(stream)
                    }
                    catch (jpe: JsonParseException)
                    {
                        Message("JPE", "", jpe.message)
                    }
                    catch (cce: ClassCastException)
                    {
                        Message("CCE", "", cce.message)
                    }
                }

            }
            return Pair(resRet, resp)
        }
    }

    fun request(req: Request) : Response
    {
        return request(req, false).second
    }
}
