package ap.mnemosyne.http

import android.app.Activity
import android.util.Log
import ap.mnemosyne.permissions.PermissionsHelper
import ap.mnemosyne.resources.Message
import ap.mnemosyne.resources.Resource
import ap.mnemosyne.resources.ResourceList
import com.fasterxml.jackson.core.JsonParseException
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayInputStream
import java.lang.ClassCastException
import java.net.UnknownHostException
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
        const val ERROR_PERMISSIONS = 999
        const val ERROR_NO_CONNECTION = 998
    }

    val act : Activity = act

    fun request(req : Request, parseRes : Boolean = false) : Pair<Resource?, Response>
    {
        if(!PermissionsHelper.checkInternetPermission(act))
        {
            //ERROR: permission are not given
            val res = mockupResponse(req, HttpHelper.ERROR_PERMISSIONS)
            return Pair(null, res)
        }

        synchronized(httpclient)
        {
            var resp : Response
            try
            {
                resp = httpclient.newCall(req).execute()
            }
            catch (uhe : UnknownHostException)
            {
                resp = mockupResponse(req, HttpHelper.ERROR_NO_CONNECTION)
                return Pair(null, resp)
            }

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

    private fun mockupResponse(req : Request, code: Int) : Response
    {
        return Response.Builder().request(req).protocol(Protocol.HTTP_1_1).message("error").code(HttpHelper.ERROR_NO_CONNECTION).build()
    }
}
