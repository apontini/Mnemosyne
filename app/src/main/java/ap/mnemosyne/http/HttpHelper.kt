package ap.mnemosyne.http

import android.content.Context
import android.util.Log
import ap.mnemosyne.R
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
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class HttpHelper(ctx: Context)
{
    companion object
    {
        val httpclient : OkHttpClient by lazy{
            val b = OkHttpClient.Builder()
            b.readTimeout(500, TimeUnit.SECONDS)
            b.writeTimeout(20, TimeUnit.SECONDS)
            return@lazy b.build()
        }

        const val BASE_URL : String = "https://pintini.ddns.net:8443"
        const val AUTH_URL : String = "$BASE_URL/mnemosyne/auth"
        const val REST_USER_URL : String = "$BASE_URL/mnemosyne/rest/user"
        const val REST_USER_PASSWORD_URL : String = "$BASE_URL/mnemosyne/rest/user/password"
        const val REST_TASK_URL : String = "$BASE_URL/mnemosyne/rest/task"
        const val PARSE_URL : String = "$BASE_URL/mnemosyne/parse"
        const val REST_PARAMETER_URL : String = "$BASE_URL/mnemosyne/rest/parameter"
        const val HINTS_URL : String = "$BASE_URL/mnemosyne/hints"
        const val ERROR_PERMISSIONS = 999
        const val ERROR_NO_CONNECTION = 998
        const val ERROR_UNKNOWN = 997
    }

    private val ctx : Context = ctx

    fun request(req : Request, parseRes : Boolean = false) : Pair<Resource?, Response>
    {
        if(!PermissionsHelper.checkInternetPermission(ctx))
        {
            //ERROR: permission are not given
            val res = mockupResponse(req, HttpHelper.ERROR_PERMISSIONS)
            return Pair(null, res)
        }

        Log.d("REQUEST", req.toString())

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
        catch (pe : java.net.ProtocolException)
        {
            resp = mockupResponse(req, HttpHelper.ERROR_UNKNOWN)
            return Pair(null, resp)
        }

        lateinit var resRet : Resource
        if(parseRes)
        {
            val bodyResp: String = resp.body()?.string() ?: Message(ctx.getString(R.string.error), "", ctx.getString(
                            R.string.error_nullResponse)).toJSON()
            Log.d("RESPONSE", bodyResp)
            if(bodyResp.contains("{\"resource-list\":["))
            {
                resRet = try
                {
                    Log.d("RESPONSE", "Parsing resource...")
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
                catch(ce : ConnectException)
                {
                    Message("ConnectException", "", ce.message)
                }
                finally
                {
                    Log.d("RESPONSE", "Parsed")
                }
            }
            else
            {
                val stream = ByteArrayInputStream(bodyResp.toByteArray(Charsets.UTF_8))
                resRet = try
                {
                    Log.d("RESPONSE", "Parsing resource...")
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
                finally
                {
                    Log.d("RESPONSE", "Parsed")
                }
            }

        }
        return Pair(resRet, resp)
    }

    private fun mockupResponse(req : Request, code: Int) : Response
    {
        return Response.Builder().request(req).protocol(Protocol.HTTP_1_1).message("error").code(HttpHelper.ERROR_NO_CONNECTION).build()
    }
}
