package ap.mnemosyne.httphandler

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import ap.mnemosyne.resources.Message
import ap.mnemosyne.resources.Resource
import com.fasterxml.jackson.core.JsonParseException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayInputStream
import java.lang.ClassCastException

class HttpHandler(act: Activity)
{
    companion object
    {
        val httpclient : OkHttpClient by lazy {
            OkHttpClient()
        }
        const val AUTH_URL : String = "http://pintini.ddns.net:8080/mnemosyne/auth"
        const val REST_USER_URL : String = "http://pintini.ddns.net:8080/mnemosyne/rest/user"
    }

    init
    {
        checkNetworkPermission(act)
    }

    fun request(req : Request, parseRes : Boolean) : Pair<Resource?, Response>
    {
        synchronized(httpclient)
        {
            val resp = httpclient.newCall(req).execute()
            var resRet : Resource? = null
            if(parseRes)
            {
                val bodyResp: String = resp.body()?.string() ?: Message("Errore", "", "null dal server").toJSON()
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

    private fun checkNetworkPermission(act: Activity)
    {
        while (ContextCompat.checkSelfPermission(act, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
        {
            val dlgAlert = AlertDialog.Builder(act)
            dlgAlert.setMessage("Please allow us to access to the internet")
            dlgAlert.setTitle("Mnemosyne")
            dlgAlert.setPositiveButton("Ok")
            {
                    dialog, which ->
            }
            dlgAlert.setCancelable(true)
            dlgAlert.create().show()
            ActivityCompat.requestPermissions(act, arrayOf(Manifest.permission.INTERNET), 0)
        }
    }


}
