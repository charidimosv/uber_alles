package com.team.eddie.uber_alles.ui.generic

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.directions.route.Route
import com.directions.route.RouteException
import com.directions.route.RoutingListener
import com.firebase.geofire.GeoLocation
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.ui.ActivityHelper
import com.team.eddie.uber_alles.utils.Status
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import com.team.eddie.uber_alles.utils.firebase.Request
import java.util.*

private const val LOCATION_PERMISSION_REQUEST_CODE = 1
private const val DEFAULT_ZOOM: Float = 15F

abstract class GenericMapFragment :
        Fragment(),
        OnMapReadyCallback,
        LocationListener,
        RoutingListener {

    /*
    ----------------------------------
    MAP
    ----------------------------------
    */

    protected lateinit var applicationContext: Context

    protected lateinit var mMap: GoogleMap

    protected var mLastLocation: Location? = null
    private var mLocationRequest: LocationRequest? = null

    protected var followMeFlag: Boolean = true

    protected lateinit var fusedLocationClient: FusedLocationProviderClient
    protected lateinit var locationCallback: LocationCallback

    private var requestingLocationUpdates: Boolean = false
    private var mLocationPermissionGranted: Boolean = false

    /*
    ----------------------------------
    USER
    ----------------------------------
    */

    protected val currentUserId: String = FirebaseHelper.getUserId()

    protected var status: Status = Status.Free

    protected var activeRequestRef: DatabaseReference? = null
    protected var activeRequestListener: ValueEventListener? = null

    protected var newIncomeMessageRef: DatabaseReference? = null
    protected var newIncomeMessageListener: ValueEventListener? = null

    protected var requestRef: DatabaseReference? = null
    protected var requestListener: ValueEventListener? = null

    protected var currentRequest: Request? = null

    protected var completedRide: Boolean = false


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        getLocationPermission()

        updateMapUI()

        getDeviceLocation()
    }

    private fun createLocationRequest() {
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

    private fun getLocationPermission() {
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

    private fun updateMapUI() {
        setLocationMapUIValues(mLocationPermissionGranted)
        mMap.isTrafficEnabled = true
    }

    @SuppressLint("MissingPermission")
    fun setLocationMapUIValues(value: Boolean) {
        mMap.isMyLocationEnabled = value
        mMap.uiSettings.isMyLocationButtonEnabled = value
    }

    override fun onLocationChanged(location: Location?) {
        if (location != null) {
            if (followMeFlag) moveCamera(location)
            saveLocation(location)
        }
    }

    fun saveLocation(location: Location) {
        mLastLocation = location

        val geoLocation = GeoLocation(location.latitude, location.longitude)
        FirebaseHelper.setUserLocation(currentUserId, geoLocation)
    }

    fun moveCamera(location: Location) {
        moveCamera(LatLng(location.latitude, location.longitude))
    }

    fun moveCamera(latlng: LatLng) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, DEFAULT_ZOOM))
    }

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

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private var polylines = arrayListOf<Polyline>()
    private val COLORS = intArrayOf(R.color.primary_dark_material_light)
    override fun onRoutingCancelled() {}

    override fun onRoutingStart() {}

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

    protected fun getRouteToMarker(fromLocation: Location, toLatLng: List<LatLng>) {
        getRouteToMarker(LatLng(fromLocation.latitude, fromLocation.longitude), toLatLng)
    }

    protected fun getRouteToMarker(fromLatLng: LatLng, toLatLng: List<LatLng>) {
        val routeList = arrayListOf(fromLatLng)
        routeList.addAll(toLatLng)
        ActivityHelper.getRouteToMarker(routeList, this)
    }

    protected fun getRouteToMarker(pickupLatLng: LatLng?) {
        if (pickupLatLng != null && mLastLocation != null)
            ActivityHelper.getRouteToMarker(arrayListOf(LatLng(mLastLocation!!.latitude, mLastLocation!!.longitude), pickupLatLng), this)
    }

    protected fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis() / 1000
    }

    protected fun setStatusSynced(status: Status) {
        this.status = status
        currentRequest?.status = status
        FirebaseHelper.updateRequest(currentRequest!!)
    }

    protected abstract fun getActiveRequest()

    protected abstract fun getRequestInfo(requestId: String)

    protected abstract fun startRideRequest()

    protected abstract fun endRideRequest()

    protected abstract fun showFreshUI()

    protected abstract fun showRatingUI()

    protected abstract fun showPendingUI()

    protected abstract fun showDriverToCustomerUI()

    protected abstract fun showRideUI()
}
