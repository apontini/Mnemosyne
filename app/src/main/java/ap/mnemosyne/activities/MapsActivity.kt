package ap.mnemosyne.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import ap.mnemosyne.R
import ap.mnemosyne.resources.Place

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_maps.*
import org.jetbrains.anko.alert
import java.lang.Exception

class MapsActivity : AppCompatActivity(), OnMapReadyCallback
{
    private var focusPlace : Place? = null
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        focusPlace = try {(intent.extras.getSerializable("focusPlace") as Place)} catch (e : Exception){null}

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
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

    override fun onMapReady(googleMap: GoogleMap)
    {
        val list = intent.getSerializableExtra("places") as HashSet<Place>
        if(!list.isEmpty())
        {
            list.forEach {
                val latlon = LatLng(it.coordinates.lat, it.coordinates.lon)
                googleMap.addMarker(MarkerOptions().position(latlon).title(it.name).title(it.name ?: it.road ?: getString(
                                    R.string.error_placeNameNotFound)))
            }

            if(focusPlace != null)
            {
                val camera = LatLng((focusPlace as Place).coordinates.lat,
                    (focusPlace as Place).coordinates.lon)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(camera, 13.0f))
            }
            else
            {
                val camera = LatLng(list.first().coordinates.lat,
                    list.first().coordinates.lon)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(camera, 13.0f))
            }
        }
        else
        {
            alert(getString(R.string.error_noPlaceGiven)) {}.show()
            finish()
        }
    }
}
