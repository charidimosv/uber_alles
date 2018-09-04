package com.team.eddie.uber_alles.ui

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import com.directions.route.*
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.utils.FirebaseHelper
import java.lang.Exception
import java.util.*

private const val LOCATION_PERMISSION_REQUEST_CODE = 1

abstract class GenericMapFragment :
        androidx.fragment.app.Fragment(),
        OnMapReadyCallback,
        LocationListener, RoutingListener {

    protected lateinit var mMap: GoogleMap

    protected var mLastLocation: Location? = null
    protected var mLocationRequest: LocationRequest? = null

    protected val currentUserId: String = FirebaseHelper.getUserId()

    protected lateinit var fusedLocationClient: FusedLocationProviderClient
    protected lateinit var locationCallback: LocationCallback

    protected var requestingLocationUpdates: Boolean = false
    protected var mLocationPermissionGranted: Boolean = false

    protected val DEFAULT_ZOOM: Float = 15F

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

    private var polylines = arrayListOf<Polyline>()
    private val COLORS = intArrayOf(R.color.primary_dark_material_light)
    override fun onRoutingCancelled() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onRoutingStart() {
    }

    override fun onRoutingFailure(p0: RouteException?) {
        if (p0 != null)
            Toast.makeText(activity!!, "Error: " + p0.message, Toast.LENGTH_LONG).show()
        else
            Toast.makeText(activity!!, "Something went wrong, Try again", Toast.LENGTH_SHORT).show()
    }

    override fun onRoutingSuccess(route: ArrayList<Route>?, p1: Int) {
        erasePolylines()
        //add route(s) to the map.
        for (i in 0 until route?.size!!) {

            //In case of more than 5 alternative routes
            val colorIndex = i % COLORS.size

            val polyOptions = PolylineOptions()
            polyOptions.color(resources.getColor(COLORS[colorIndex], resources.newTheme()))
            polyOptions.width((10 + i * 3).toFloat())
            polyOptions.addAll(route[i].points)
            val polyline = mMap.addPolyline(polyOptions)
            polylines.add(polyline)

            Toast.makeText(activity!!, "Route " + (i + 1) + ": distance - " + route.get(i).distanceValue + ": duration - " + route.get(i).durationValue, Toast.LENGTH_SHORT).show()
        }
    }

    protected fun erasePolylines() {
        for (line in polylines)
            line.remove()
    }

    protected fun getRouteToMarker(pickupLatLng: LatLng?) {
        if (pickupLatLng != null && mLastLocation != null) {
            val routing = Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(false)
                    .waypoints(LatLng(mLastLocation!!.latitude, mLastLocation!!.longitude), pickupLatLng)
                    .build()
            routing.execute()
        }
    }

}
