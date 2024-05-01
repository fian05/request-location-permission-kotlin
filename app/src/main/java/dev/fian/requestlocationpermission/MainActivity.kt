package dev.fian.requestlocationpermission

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class MainActivity : AppCompatActivity() {

    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0

    private lateinit var textViewLocation: TextView
    private lateinit var buttonLocation: Button
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val REQUEST_LOCATION_PERMISSION = 1
    private val REQUEST_CHECK_SETTINGS = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textViewLocation = findViewById(R.id.textView_location)
        buttonLocation = findViewById(R.id.button_location)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        dapetinLatLong()
        buttonLocation.setOnClickListener {
            bukaGMaps()
        }
    }

    override fun onResume() {
        super.onResume()
        requestLocation()
        dapetinLatLong()
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun bukaGMaps() {
        val finalUrl = "geo:?q=$currentLatitude,$currentLongitude"
        val coordinates = extractCoordinatesFromUrl(finalUrl)
        if (coordinates != null) {
            val gmmIntentUri = Uri.parse(finalUrl)
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        }
    }

    private fun extractCoordinatesFromUrl(url : String) : String? {
        val uri = Uri.parse(url)
        if (uri.scheme == "geo" && uri.schemeSpecificPart != null) {
            val coordinates = uri.schemeSpecificPart.split(",")
            if (coordinates.size >= 2) {
                val latitude = coordinates[0]
                val longitude = coordinates[1]
                return "$latitude,$longitude"
            }
        }
        return null
    }

    private fun dapetinLatLong() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    showLocation(it)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Failed to get location: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun requestLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_LOCATION_PERMISSION
                )
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_LOCATION_PERMISSION
                )
            }
        } else {
            checkAndEnableLocationServices()
        }
    }

    private fun checkAndEnableLocationServices() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled || !isNetworkEnabled) {
            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

            val client: SettingsClient = LocationServices.getSettingsClient(this)
            val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

            task.addOnCompleteListener { result ->
                try {
                    result.getResult(ApiException::class.java)
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return@addOnCompleteListener
                    }
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { location: Location? ->
                            location?.let {
                                showLocation(it)
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Failed to get location: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } catch (exception: ApiException) {
                    if (exception.statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                        try {
                            val resolvable: ResolvableApiException = exception as ResolvableApiException
                            resolvable.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                        } catch (e: IntentSender.SendIntentException) {
                            Toast.makeText(
                                this,
                                "Failed to show location settings dialog.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: ClassCastException) {
                            Toast.makeText(this, "Failed to resolve location settings.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showLocation(location: Location) {
        currentLatitude = location.latitude
        currentLongitude = location.longitude
        textViewLocation.text = "Latitude: $currentLatitude\nLongitude: $currentLongitude"
    }
}
