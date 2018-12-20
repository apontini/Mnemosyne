package ap.mnemosyne.activities

import android.app.Activity
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
import ap.mnemosyne.parameters.ParametersHelper
import ap.mnemosyne.resources.LocationParameter
import ap.mnemosyne.resources.TimeParameter
import kotlinx.android.synthetic.main.activity_onboarding.*
import kotlinx.android.synthetic.main.fragment_onboarding_2.*
import kotlinx.android.synthetic.main.fragment_onboarding_3.*
import org.joda.time.format.DateTimeFormat
import java.util.*


class OnboardingActivity : AppCompatActivity()
{

    companion object
    {
        const val ONBOARDING_REQUEST_CODE = 101
        var menuEnabled = true
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_onboarding)

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
            var ok = false
        }

        private lateinit var params : ParametersHelper

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
        {
            return inflater.inflate(R.layout.fragment_onboarding_2, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?)
        {
            super.onViewCreated(view, savedInstanceState)
            super.onActivityCreated(savedInstanceState)
            params = ParametersHelper(activity?.parent ?: activity as Activity)
            val house = params.getLocalParameter(ParamsName.location_house)
            val work = params.getLocalParameter(ParamsName.location_work)

            val gcd = Geocoder(context, Locale.getDefault())
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
                Log.d("BUTTON", "Pressed")
            }

            buttonWorkplace.setOnClickListener {
                Log.d("BUTTON", "Pressed")
            }
        }
    }

    class Page3Fragment : Fragment()
    {
        companion object
        {
            var ok = false
        }

        private lateinit var params : ParametersHelper

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
        {
            return inflater.inflate(R.layout.fragment_onboarding_3, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?)
        {
            super.onViewCreated(view, savedInstanceState)
            super.onActivityCreated(savedInstanceState)
            params = ParametersHelper(activity?.parent ?: activity as Activity)
            val lunchTime = params.getLocalParameter(ParamsName.time_lunch)
            val dinnerTime = params.getLocalParameter(ParamsName.time_dinner)
            val bedTime = params.getLocalParameter(ParamsName.time_bed)
            val workTime = params.getLocalParameter(ParamsName.time_work)

            val gcd = Geocoder(context, Locale.getDefault())
            lateinit var addresses : MutableList<Address>


            lunch.text = if(lunchTime != null) "${(lunchTime as TimeParameter).fromTime.toString(
                DateTimeFormat.forPattern("HH:mm"))} - ${(lunchTime as TimeParameter).toTime.toString(DateTimeFormat.forPattern("HH:mm"))}"
                else getString(R.string.text_settings_notDefined)

            dinner.text = if(dinnerTime != null) "${(dinnerTime as TimeParameter).fromTime.toString(
                DateTimeFormat.forPattern("HH:mm"))} - ${(dinnerTime as TimeParameter).toTime.toString(DateTimeFormat.forPattern("HH:mm"))}"
            else getString(R.string.text_settings_notDefined)

            bed.text = if(bedTime != null) "${(bedTime as TimeParameter).fromTime.toString(
                DateTimeFormat.forPattern("HH:mm"))} - ${(bedTime as TimeParameter).toTime.toString(DateTimeFormat.forPattern("HH:mm"))}"
            else getString(R.string.text_settings_notDefined)

            work.text = if(workTime != null) "${(workTime as TimeParameter).fromTime.toString(
                DateTimeFormat.forPattern("HH:mm"))} - ${(workTime as TimeParameter).toTime.toString(DateTimeFormat.forPattern("HH:mm"))}"
            else getString(R.string.text_settings_notDefined)

            if(workTime != null && bedTime != null && lunchTime != null && dinnerTime != null)
            {
                Page3Fragment.ok = true
            }

            buttonDinner.setOnClickListener {
                Log.d("BUTTON", "Pressed")
            }

            buttonLunch.setOnClickListener {
                Log.d("BUTTON", "Pressed")
            }

            buttonBed.setOnClickListener {
                Log.d("BUTTON", "Pressed")
            }

            buttonWork.setOnClickListener {
                Log.d("BUTTON", "Pressed")
            }
        }
    }

    class Page4Fragment : Fragment()
    {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
        {
            return inflater.inflate(R.layout.fragment_onboarding_4, container, false)
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
                1 -> Page2Fragment()
                2 -> Page3Fragment()
                3 -> Page4Fragment()
                else -> Page1Fragment()
            }
        }
    }
}
