package com.example.vagabound

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location 
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button // Added import
import android.widget.Toast // Added import
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.applicationContext)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_container) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val recenterButton: Button = findViewById(R.id.recenter_button)
        recenterButton.setOnClickListener {
            // Check if mMap has been initialized
            if (::mMap.isInitialized) {
                val facts = FactRepository.getFactsNearLocation(0.0, 0.0, 1.0) // Dummy values
                if (facts.isNotEmpty()) {
                    val firstFactPosition = LatLng(facts[0].latitude, facts[0].longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(firstFactPosition, 12f))
                    Toast.makeText(this, "Centering on mock facts", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No mock facts to center on", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Map not ready yet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        Log.d(TAG, "Map is ready.")
        checkLocationPermissions() 
        displayFactMarkers()       
    }

    private fun displayFactMarkers() {
        val facts = FactRepository.getFactsNearLocation(0.0, 0.0, 1.0) 

        for (fact in facts) {
            val factPosition = LatLng(fact.latitude, fact.longitude)
            mMap.addMarker(
                MarkerOptions()
                    .position(factPosition)
                    .title(fact.title)
                    .snippet(fact.details)
            )
            Log.d(TAG, "Added marker for: ${fact.title}")
        }

        if (facts.isNotEmpty()) {
            val firstFactPosition = LatLng(facts[0].latitude, facts[0].longitude)
            // Check if mMap is initialized before using it here, though it should be by now.
            if (::mMap.isInitialized) {
                 mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstFactPosition, 12f)) 
                 Log.d(TAG, "Moved camera to first fact: ${facts[0].title}")
            }
        }
    }

    private fun checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            Log.d(TAG, "Location permission already granted.")
            getCurrentLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted by user.")
                getCurrentLocation()
            } else {
                Log.d(TAG, "Location permission denied by user.")
                Toast.makeText(this, "Location permission denied. Showing default map location.", Toast.LENGTH_SHORT).show()
                if (::mMap.isInitialized) {
                    val sydney = LatLng(-33.852, 151.211)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 10f))
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Permission not granted in getCurrentLocation. Defaulting map.")
            Toast.makeText(this, "Location permission not granted. Showing default map location.", Toast.LENGTH_LONG).show()
            if (::mMap.isInitialized) {
                val sydney = LatLng(-33.852, 151.211)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 10f))
            }
            return
        }
        
        Log.d(TAG, "Location fetching temporarily disabled due to compilation issue. MyLocation layer not enabled.")
        // Toast.makeText(this, "Location fetching disabled. Map will not move to current location.", Toast.LENGTH_LONG).show()
        // mMap.isMyLocationEnabled = true // Temporarily commented out

        /*
        fusedLocationClient.lastKnownLocation
            .addOnSuccessListener { location: Location? -> 
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    Log.d(TAG, "User Location: $userLatLng")
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                } else {
                    Log.d(TAG, "Last known location is null. Consider requesting current location.")
                    Toast.makeText(this, "Last known location is null.", Toast.LENGTH_SHORT).show()
                    val sydney = LatLng(-33.852, 151.211)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 10f))
                }
            }
            .addOnFailureListener { e: Exception -> 
                Log.e(TAG, "Error getting location", e)
                Toast.makeText(this, "Error getting location.", Toast.LENGTH_SHORT).show()
                val sydney = LatLng(-33.852, 151.211)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 10f))
            }
        */
        
        if (::mMap.isInitialized && FactRepository.getFactsNearLocation(0.0,0.0,0.0).isEmpty()) {
             val sydney = LatLng(-33.852, 151.211)
             mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 10f))
             Log.d(TAG, "getCurrentLocation: Map moved to default (Sydney) as location fetching is off and no facts to focus on.")
        }
    }
}
