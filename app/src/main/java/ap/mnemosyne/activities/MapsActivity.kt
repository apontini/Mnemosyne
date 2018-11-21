package ap.mnemosyne.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import ap.mnemosyne.resources.Place
import apontini.mnemosyne.R

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.jetbrains.anko.alert

class MapsActivity : AppCompatActivity(), OnMapReadyCallback
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap)
    {
        val list = intent.getSerializableExtra("places") as HashSet<Place>
        if(!list.isEmpty())
        {
            list.forEach {
                val latlon = LatLng(it.coordinates.lat, it.coordinates.lon)
                googleMap.addMarker(MarkerOptions().position(latlon).title(it.name).title(it.name ?: "Nome non trovato"))
            }

            val camera = LatLng(list.first().coordinates.lat, list.first().coordinates.lon)
            if(list.size == 1)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(camera, 15.0f))
            else
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(camera, 10.0f))
        }
        else
        {
            alert("Nessun posto indicato") {}.show()
            finish()
        }
    }
}
