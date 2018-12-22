package ap.mnemosyne.activities

import android.app.Activity
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import ap.mnemosyne.R
import ap.mnemosyne.enums.ParamsName
import ap.mnemosyne.http.HttpHelper
import ap.mnemosyne.parameters.ParametersHelper
import ap.mnemosyne.resources.LocationParameter
import ap.mnemosyne.resources.Message
import ap.mnemosyne.resources.Point
import ap.mnemosyne.resources.TimeParameter
import ap.mnemosyne.session.SessionHelper
import com.google.android.gms.location.places.ui.PlacePicker
import kotlinx.android.synthetic.main.activity_onboarding.*
import kotlinx.android.synthetic.main.fragment_onboarding_2.*
import kotlinx.android.synthetic.main.fragment_onboarding_3.*
import kotlinx.android.synthetic.main.fragment_onboarding_4.*
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.jetbrains.anko.alert
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat
import java.util.*


class OnboardingActivity : AppCompatActivity()
{

    companion object
    {
        const val ONBOARDING_REQUEST_CODE = 101
        var menuEnabled = true
    }

    private lateinit var session : SessionHelper
    private lateinit var params : ParametersHelper

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_onboarding)
        session = SessionHelper(this)
        params = ParametersHelper(this)

        val pagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager)
        pager.adapter = pagerAdapter

        setSupportActionBar(bar)

        pager.setOnTouchListener { view, motionEvent ->
            true
        }

        pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener
        {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int)
            {
            }

            override fun onPageSelected(position: Int)
            {
                when(position)
                {
                    1->
                    {
                        if(Page2Fragment.ok)
                        {
                            menuEnabled = true
                            invalidateOptionsMenu()
                        }
                        else
                        {
                            menuEnabled = false
                            invalidateOptionsMenu()
                        }
                    }

                    2->
                    {
                        if(Page3Fragment.ok)
                        {
                            menuEnabled = true
                            invalidateOptionsMenu()
                        }
                        else
                        {
                            menuEnabled = false
                            invalidateOptionsMenu()
                        }
                    }
                }
            }

            override fun onPageScrollStateChanged(state: Int)
            {
            }
        })
    }

    override fun onBackPressed()
    {
        if(pager.currentItem > 0) pager.currentItem = pager.currentItem -1
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_onboarding, menu)
        menu.findItem(R.id.action_next).isEnabled = menuEnabled
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        return when (item.itemId)
        {
            R.id.action_next -> {
                pager.currentItem = pager.currentItem + 1
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    class Page1Fragment : Fragment()
    {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
        {
            return inflater.inflate(R.layout.fragment_onboarding_1, container, false)
        }
    }

    class Page2Fragment : Fragment()
    {

        companion object
        {
            fun newInstance(session: SessionHelper, params: ParametersHelper): Page2Fragment
            {
                val fragment = Page2Fragment()
                val args = Bundle()
                args.putSerializable("session", session)
                args.putSerializable("params", params)
                fragment.arguments = args
                return fragment
            }
            var ok = false
        }

        private lateinit var params : ParametersHelper
        private lateinit var session: SessionHelper
        private lateinit var gcd : Geocoder

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
        {
            params = arguments?.getSerializable("params") as ParametersHelper
            session = arguments?.getSerializable("session") as SessionHelper
            return inflater.inflate(R.layout.fragment_onboarding_2, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?)
        {
            super.onViewCreated(view, savedInstanceState)
            super.onActivityCreated(savedInstanceState)

            val house = params.getLocalParameter(ParamsName.location_house)
            val work = params.getLocalParameter(ParamsName.location_work)

            gcd = Geocoder(context, Locale.getDefault())
            lateinit var addresses : MutableList<Address>

            if(house != null)
            {
                addresses = gcd.getFromLocation((house as LocationParameter).location?.lat!!, (house).location?.lon!!, 1)
                if (addresses.size > 0)
                {
                    if(addresses[0].thoroughfare != null)
                    {
                        abitazione.text = addresses[0].thoroughfare
                    }
                }
            }

            if(work != null)
            {
                addresses = gcd.getFromLocation((work as LocationParameter).location?.lat!!, (work).location?.lon!!, 1)
                if (addresses.size > 0)
                {
                    if(addresses[0].thoroughfare != null)
                    {
                        workplace.text = addresses[0].thoroughfare
                    }
                }
            }

            if(work != null && house != null)
            {
                ok = true
            }

            buttonHouse.setOnClickListener {
                val builder: PlacePicker.IntentBuilder = PlacePicker.IntentBuilder()
                startActivityForResult(builder.build(this.activity), 0)
            }

            buttonWorkplace.setOnClickListener {
                val builder: PlacePicker.IntentBuilder = PlacePicker.IntentBuilder()
                startActivityForResult(builder.build(this.activity), 1)
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
        {
            super.onActivityResult(requestCode, resultCode, data)

            when (requestCode)
            {
                0,1 ->
                {
                    val paramName = when (requestCode)
                    {
                        0 -> ParamsName.location_house
                        1 -> ParamsName.location_work
                        else -> null
                    }

                    if(paramName != null && resultCode == Activity.RESULT_OK)
                    {
                        val bRequest = Request.Builder()
                            .addHeader("Cookie" , "JSESSIONID="+session.user.sessionID)
                            .url(HttpHelper.REST_PARAMETER_URL)

                        when (requestCode)
                        {
                            0, 1 ->
                            {
                                val place = PlacePicker.getPlace(this.activity, data)
                                Log.d("PARAMETER", "Creating parameter")
                                val param = LocationParameter(paramName, session.user.email, Point(place.latLng.latitude, place.latLng.longitude), -1, null)
                                if((this@Page2Fragment.activity as Activity).defaultSharedPreferences.getString(
                                        if(requestCode == 0) ParamsName.location_house.toString() else ParamsName.location_work.toString(),
                                        "") != "")
                                {
                                    bRequest.put(RequestBody.create(MediaType.parse("application/json"), param.toJSON()))
                                }
                                else
                                {
                                    bRequest.post(RequestBody.create(MediaType.parse("application/json"), param.toJSON()))
                                }
                            }
                        }

                        doAsync {
                            val resp = HttpHelper((this@Page2Fragment.activity as Activity)).request(bRequest.build(), true)
                            when(resp.second.code())
                            {
                                201, 200 ->
                                {
                                    when (requestCode)
                                    {
                                        0, 1 ->
                                        {
                                            with((this@Page2Fragment.activity as Activity).defaultSharedPreferences.edit())
                                            {
                                                putString(paramName.name, (resp.first as LocationParameter).toJSON())
                                                apply()
                                            }

                                            when(requestCode)
                                            {
                                                0 ->
                                                {
                                                    val addresses = gcd.getFromLocation((resp.first as LocationParameter).location.lat, (resp.first as LocationParameter).location.lon, 1)
                                                    if (addresses.size > 0)
                                                    {
                                                        if(addresses[0].thoroughfare != null)
                                                        {
                                                            uiThread { abitazione.text = addresses[0].thoroughfare }
                                                        }
                                                    }
                                                }

                                                1 ->
                                                {
                                                    val addresses = gcd.getFromLocation((resp.first as LocationParameter).location.lat, (resp.first as LocationParameter).location.lon, 1)
                                                    if (addresses.size > 0)
                                                    {
                                                        if(addresses[0].thoroughfare != null)
                                                        {
                                                            uiThread { workplace.text = addresses[0].thoroughfare }
                                                        }
                                                    }
                                                }
                                            }

                                            if(params.getLocalParameter(ParamsName.location_house) != null && params.getLocalParameter(ParamsName.location_work) != null)
                                            {
                                                Page2Fragment.ok = true
                                                menuEnabled = true
                                                (this@Page2Fragment.activity as Activity).invalidateOptionsMenu()
                                            }
                                        }
                                    }
                                }

                                401 ->
                                {
                                    Log.d("SESSION", "Sessione scaduta")
                                    val intent = Intent(this@Page2Fragment.activity, LoginActivity::class.java)
                                    startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
                                }

                                400 ->
                                {
                                    uiThread {
                                        (this@Page2Fragment.activity as Activity).alert("400: " + (resp.first as Message).errorDetails).show()
                                    }
                                }

                                500 ->
                                {
                                    uiThread {
                                        (this@Page2Fragment.activity as Activity).alert("500: " + (resp.first as Message).errorDetails).show()
                                    }
                                }

                                HttpHelper.ERROR_PERMISSIONS ->
                                {
                                    uiThread {
                                        (this@Page2Fragment.activity as Activity).alert((this@Page2Fragment.activity as Activity).getString(R.string.alert_noInternetPermission)).show()
                                    }
                                }

                                HttpHelper.ERROR_NO_CONNECTION ->
                                {
                                    uiThread {
                                        (this@Page2Fragment.activity as Activity).alert((this@Page2Fragment.activity as Activity).getString(R.string.alert_noInternetConnection)).show()
                                    }
                                }

                                else ->
                                {
                                    uiThread {
                                        (this@Page2Fragment.activity as Activity).alert(getString(R.string.text_general_error, (resp.first as Message).errorDetails)).show()
                                    }
                                }
                            }
                        }
                    }
                }

                SessionHelper.LOGIN_REQUEST_CODE->{
                    if(resultCode == Activity.RESULT_OK)
                    {
                        //supportActionBar?.customView?.snackbar("Sei collegato come: " + session.user.email)?.show()
                    }
                    else
                    {
                        val intent = Intent(this@Page2Fragment.activity, LoginActivity::class.java)
                        startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
                    }
                }
            }
        }
    }

    class Page3Fragment : Fragment()
    {
        companion object
        {
            fun newInstance(session: SessionHelper, params: ParametersHelper): Page3Fragment
            {
                val fragment = Page3Fragment()
                val args = Bundle()
                args.putSerializable("session", session)
                args.putSerializable("params", params)
                fragment.arguments = args
                return fragment
            }
            var ok = false
        }

        private lateinit var params : ParametersHelper
        private lateinit var session: SessionHelper

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
        {
            params = arguments?.getSerializable("params") as ParametersHelper
            session = arguments?.getSerializable("session") as SessionHelper
            return inflater.inflate(R.layout.fragment_onboarding_3, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?)
        {
            super.onViewCreated(view, savedInstanceState)
            super.onActivityCreated(savedInstanceState)
            val lunchTime = params.getLocalParameter(ParamsName.time_lunch)
            val dinnerTime = params.getLocalParameter(ParamsName.time_dinner)
            val bedTime = params.getLocalParameter(ParamsName.time_bed)
            val workTime = params.getLocalParameter(ParamsName.time_work)

            val gcd = Geocoder(context, Locale.getDefault())
            lateinit var addresses : MutableList<Address>


            lunch.text = if(lunchTime != null) "${(lunchTime as TimeParameter).fromTime.toString(
                DateTimeFormat.forPattern("HH:mm"))} - ${(lunchTime).toTime.toString(DateTimeFormat.forPattern("HH:mm"))}"
                else getString(R.string.text_settings_notDefined)

            dinner.text = if(dinnerTime != null) "${(dinnerTime as TimeParameter).fromTime.toString(
                DateTimeFormat.forPattern("HH:mm"))} - ${(dinnerTime).toTime.toString(DateTimeFormat.forPattern("HH:mm"))}"
            else getString(R.string.text_settings_notDefined)

            bed.text = if(bedTime != null) "${(bedTime as TimeParameter).fromTime.toString(
                DateTimeFormat.forPattern("HH:mm"))} - ${(bedTime).toTime.toString(DateTimeFormat.forPattern("HH:mm"))}"
            else getString(R.string.text_settings_notDefined)

            work.text = if(workTime != null) "${(workTime as TimeParameter).fromTime.toString(
                DateTimeFormat.forPattern("HH:mm"))} - ${(workTime).toTime.toString(DateTimeFormat.forPattern("HH:mm"))}"
            else getString(R.string.text_settings_notDefined)

            if(workTime != null && bedTime != null && lunchTime != null && dinnerTime != null)
            {
                Page3Fragment.ok = true
            }

            buttonDinner.setOnClickListener {
                val intent = Intent(this.activity, TimeIntervalPickerActivity::class.java)
                startActivityForResult(intent, 3)
            }

            buttonLunch.setOnClickListener {
                val intent = Intent(this.activity, TimeIntervalPickerActivity::class.java)
                startActivityForResult(intent, 2)
            }

            buttonBed.setOnClickListener {
                val intent = Intent(this.activity, TimeIntervalPickerActivity::class.java)
                startActivityForResult(intent, 4)
            }

            buttonWork.setOnClickListener {
                val intent = Intent(this.activity, TimeIntervalPickerActivity::class.java)
                startActivityForResult(intent, 5)
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
        {
            super.onActivityResult(requestCode, resultCode, data)

            when (requestCode)
            {
                2,3,4,5 ->
                {
                    val paramName = when (requestCode)
                    {
                        2 -> ParamsName.time_lunch
                        3 -> ParamsName.time_dinner
                        4 -> ParamsName.time_bed
                        5 -> ParamsName.time_work
                        else -> null
                    }

                    if(paramName != null && resultCode == Activity.RESULT_OK)
                    {
                        val bRequest = Request.Builder()
                            .addHeader("Cookie" , "JSESSIONID=" + session.user.sessionID)
                            .url(HttpHelper.REST_PARAMETER_URL)

                        when (requestCode)
                        {
                            2,3,4,5 ->
                            {
                                val fromTime = data?.getSerializableExtra("fromTime") as LocalTime
                                val toTime = data?.getSerializableExtra("toTime") as LocalTime
                                Log.d("PARAMETER", "Creating parameter")
                                val param = TimeParameter(paramName, session.user.email, fromTime, toTime)
                                if((this@Page3Fragment.activity as Activity).defaultSharedPreferences.getString(
                                    when (requestCode) {
                                        2 -> ParamsName.time_lunch.toString()
                                        3 -> ParamsName.time_dinner.toString()
                                        4 -> ParamsName.time_bed.toString()
                                        else -> ParamsName.time_work.toString()
                                    },
                                    "") != "") {
                                    bRequest.put(
                                        RequestBody.create(
                                            MediaType.parse("application/json"),
                                            param.toJSON()
                                        )
                                    )
                                }
                                else
                                {
                                    bRequest.post(
                                        RequestBody.create(
                                            MediaType.parse("application/json"),
                                            param.toJSON()
                                        )
                                    )
                                }
                            }
                        }

                        doAsync {
                            val resp = HttpHelper(this@Page3Fragment.activity as Activity).request(bRequest.build(), true)
                            when(resp.second.code())
                            {
                                201, 200 ->
                                {
                                    when (requestCode)
                                    {
                                        2,3,4,5 ->
                                        {
                                            with((this@Page3Fragment.activity as Activity).defaultSharedPreferences.edit())
                                            {
                                                putString(paramName.name, (resp.first as TimeParameter).toJSON())
                                                apply()
                                            }
                                            when(requestCode)
                                            {
                                                //CAMBIARE FORMATO
                                                2->
                                                {
                                                    uiThread {
                                                        lunch.text = "${(data?.getSerializableExtra("fromTime") as LocalTime).toString(
                                                            DateTimeFormat.forPattern("HH:mm"))} - ${(data?.getSerializableExtra("toTime") as LocalTime).toString(
                                                            DateTimeFormat.forPattern("HH:mm"))}"
                                                    }
                                                }

                                                3->
                                                {
                                                    uiThread {
                                                        dinner.text = "${(data?.getSerializableExtra("fromTime") as LocalTime).toString(
                                                            DateTimeFormat.forPattern("HH:mm"))} - ${(data?.getSerializableExtra("toTime") as LocalTime).toString(
                                                            DateTimeFormat.forPattern("HH:mm"))}"
                                                    }
                                                }

                                                4->
                                                {
                                                    uiThread {
                                                        bed.text = "${(data?.getSerializableExtra("fromTime") as LocalTime).toString(
                                                            DateTimeFormat.forPattern("HH:mm"))} - ${(data?.getSerializableExtra("toTime") as LocalTime).toString(
                                                            DateTimeFormat.forPattern("HH:mm"))}"
                                                    }

                                                }

                                                5->
                                                {
                                                    uiThread {
                                                        work.text = "${(data?.getSerializableExtra("fromTime") as LocalTime).toString(
                                                            DateTimeFormat.forPattern("HH:mm"))} - ${(data?.getSerializableExtra("toTime") as LocalTime).toString(
                                                            DateTimeFormat.forPattern("HH:mm"))}"
                                                    }
                                                }
                                            }

                                            if(params.getLocalParameter(ParamsName.time_work) != null &&
                                                params.getLocalParameter(ParamsName.time_bed) != null &&
                                                params.getLocalParameter(ParamsName.time_dinner) != null &&
                                                params.getLocalParameter(ParamsName.time_lunch) != null)
                                            {
                                                Page3Fragment.ok = true
                                                menuEnabled = true
                                                (this@Page3Fragment.activity as Activity).invalidateOptionsMenu()
                                            }
                                        }
                                    }
                                }

                                401 ->
                                {
                                    Log.d("SESSION", "Sessione scaduta")
                                    val intent = Intent(this@Page3Fragment.activity as Activity, LoginActivity::class.java)
                                    startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
                                }

                                400 ->
                                {
                                    uiThread {
                                        (this@Page3Fragment.activity as Activity).alert("400: " + (resp.first as Message).errorDetails).show()
                                    }
                                }

                                500 ->
                                {
                                    uiThread {
                                        (this@Page3Fragment.activity as Activity).alert("500: " + (resp.first as Message).errorDetails).show()
                                    }
                                }

                                HttpHelper.ERROR_PERMISSIONS ->
                                {
                                    uiThread {
                                        (this@Page3Fragment.activity as Activity).alert((this@Page3Fragment.activity as Activity).getString(R.string.alert_noInternetPermission)).show()
                                    }
                                }

                                HttpHelper.ERROR_NO_CONNECTION ->
                                {
                                    uiThread {
                                        (this@Page3Fragment.activity as Activity).alert((this@Page3Fragment.activity as Activity).getString(R.string.alert_noInternetConnection)).show()
                                    }
                                }

                                else ->
                                {
                                    uiThread {
                                        (this@Page3Fragment.activity as Activity).alert(getString(R.string.text_general_error, (resp.first as Message).errorDetails)).show()
                                    }
                                }
                            }
                        }
                    }
                }

                SessionHelper.LOGIN_REQUEST_CODE->{
                    if(resultCode == Activity.RESULT_OK)
                    {
                        //(this@Page3Fragment.activity as Activity).supportActionBar?.customView?.snackbar("Sei collegato come: " + session.user.email)?.show()
                    }
                    else
                    {
                        val intent = Intent(this@Page3Fragment.activity, LoginActivity::class.java)
                        startActivityForResult(intent, SessionHelper.LOGIN_REQUEST_CODE)
                    }
                }
            }
        }
    }

    class Page4Fragment : Fragment()
    {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
        {
            return inflater.inflate(R.layout.fragment_onboarding_4, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?)
        {
            buttonBegin.setOnClickListener {
                val returnIntent = Intent()
                this.activity?.setResult(Activity.RESULT_OK, returnIntent)
                this.activity?.finish()
            }
            super.onViewCreated(view, savedInstanceState)
        }
    }

    private inner class ScreenSlidePagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm)
    {
        override fun getCount(): Int = 4

        override fun getItem(position: Int): Fragment
        {
            return when (position)
            {
                0 -> Page1Fragment()
                1 -> Page2Fragment.newInstance(session, params)
                2 -> Page3Fragment.newInstance(session, params)
                3 -> Page4Fragment()
                else -> Page1Fragment()
            }
        }
    }
}
