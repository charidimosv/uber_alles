package com.team.eddie.uber_alles.ui

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import java.lang.Exception

private const val LOCATION_PERMISSION_REQUEST_CODE = 1

abstract class GenericMapFragment :
        androidx.fragment.app.Fragment(),
        OnMapReadyCallback,
        LocationListener {

    protected lateinit var mMap: GoogleMap

    protected var mLastLocation: Location? = null
    protected var mLocationRequest: LocationRequest? = null

    protected lateinit var fusedLocationClient: FusedLocationProviderClient
    protected lateinit var locationCallback: LocationCallback

    protected var requestingLocationUpdates: Boolean = false
    protected var mLocationPermissionGranted: Boolean = false

    // sydney
    protected val mDefaultLocation = LatLng(-33.8523341, 151.2106085)
    protected val DEFAULT_ZOOM: Float = 15F

    protected val REQUESTING_LOCATION_UPDATES_KEY = "REQUESTING_LOCATION_UPDATES"

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        getLocationPermission()

        updateMapUI()

        getDeviceLocation()
    }

    protected fun createLocationRequest() {
        mLocationRequest = LocationRequest().apply {
            interval = 4000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val locationSettingsRequest = LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest!!)
                .build()

        LocationServices.getSettingsClient(activity!!).checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener {
                    requestingLocationUpdates = true
                    startLocationUpdates()
                }
                .addOnFailureListener { exception ->
                    if (exception is ResolvableApiException) {
                    }
                }
    }

    protected fun getLocationPermission() {
        if (ActivityCompat.checkSelfPermission(activity!!, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(activity!!,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE)
        else
            mLocationPermissionGranted = true
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            @NonNull permissions: Array<String>,
                                            @NonNull grantResults: IntArray) {
        mLocationPermissionGranted = false
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE ->
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    mLocationPermissionGranted = true
        }

        setLocationMapUIValues(mLocationPermissionGranted)
    }

    @SuppressLint("MissingPermission")
    protected fun getDeviceLocation() {
        if (mLocationPermissionGranted) {
            fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? -> onLocationChanged(location) }
                    .addOnFailureListener { e: Exception -> Log.e("Exception: %s", e.message) }

            createLocationRequest()
        }
    }

    protected fun updateMapUI() {
        setLocationMapUIValues(mLocationPermissionGranted)
        mMap.isTrafficEnabled = true
    }

    @SuppressLint("MissingPermission")
    fun setLocationMapUIValues(value: Boolean) {
        mMap.isMyLocationEnabled = value
        mMap.uiSettings.isMyLocationButtonEnabled = value
    }

    abstract override fun onLocationChanged(location: Location?)

    override fun onResume() {
        super.onResume()
        if (requestingLocationUpdates) startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    protected fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, null)
    }

    protected fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

}