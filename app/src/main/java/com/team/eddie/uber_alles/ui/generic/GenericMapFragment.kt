package com.team.eddie.uber_alles.ui.generic

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.firebase.geofire.GeoLocation
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.places.Place
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.utils.Status
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import com.team.eddie.uber_alles.utils.firebase.Request
import com.team.eddie.uber_alles.utils.map.MapRouteHelper
import java.util.*

private const val LOCATION_PERMISSION_REQUEST_CODE = 1
private const val DEFAULT_ZOOM: Float = 15F

abstract class GenericMapFragment :
        Fragment(),
        OnMapReadyCallback,
        LocationListener {

    /*
    ----------------------------------
    MAP
    ----------------------------------
    */

    protected lateinit var applicationContext: Context

    protected lateinit var mMap: GoogleMap
    private lateinit var mapHelper: MapRouteHelper

    protected var mLastLocation: Location? = null
    private var mLocationRequest: LocationRequest? = null

    protected var followMeFlag: Boolean = true

    protected lateinit var fusedLocationClient: FusedLocationProviderClient
    protected lateinit var locationCallback: LocationCallback

    private var requestingLocationUpdates: Boolean = false
    private var mLocationPermissionGranted: Boolean = false

    /*
    ----------------------------------
    UI
    ----------------------------------
    */

    protected lateinit var popup: LinearLayout

    protected lateinit var destination: TextView
    protected lateinit var userAllInfo: LinearLayout

    protected lateinit var userInfo: LinearLayout
    protected lateinit var userProfileImage: ImageView
    protected lateinit var userName: TextView
    protected lateinit var userPhone: TextView

    protected lateinit var currentRating: LinearLayout
    protected lateinit var currentRatingBar: RatingBar
    protected lateinit var currentRatingAvg: TextView

    protected lateinit var newRating: LinearLayout
    protected lateinit var newRatingBar: RatingBar
    protected lateinit var newRatingText: TextInputEditText
    protected lateinit var newRatingButton: MaterialButton

    protected lateinit var communicateUser: LinearLayout
    protected lateinit var callUser: MaterialButton
    protected lateinit var chatUser: MaterialButton

    protected lateinit var rideStatus: MaterialButton

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

    protected var successfulRide: Boolean = false
    protected var showMessages: Boolean = false

    protected val destinationMap: HashMap<Marker, Place> = HashMap()
    protected val destinationList: ArrayList<Place> = ArrayList()


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mapHelper = MapRouteHelper(mMap)

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
        mMap.isTrafficEnabled = false
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

    private fun saveLocation(location: Location) {
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

    protected fun syncRequestDestination() {
        clearDestinationInfo()

        destinationList.addAll(currentRequest!!.destinationList!!)
        for (place in destinationList) {
            val destinationMarker = mMap.addMarker(MarkerOptions()
                    .position(place.latLng)
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)))
            destinationMap[destinationMarker] = place
        }
    }

    protected fun clearDestinationInfo() {
        for (marker in destinationMap.keys) marker.remove()
        destinationMap.clear()
        destinationList.clear()
    }

    protected fun getDestinationLatLngList(): ArrayList<LatLng> {
        val latLngList = ArrayList<LatLng>()
        for (place in destinationList) latLngList.add(place.latLng)
        return latLngList
    }

    protected fun getDestinationAsString(): String {
        var destinationAll: String = ""
        for (place in currentRequest?.destinationList!!)
            destinationAll = destinationAll + place.address + " "
        return destinationAll
    }

    protected fun createMarkerRoute(from: LatLng, to: List<LatLng>) {
        cleanMarkerRoute()

        val locationStopList: ArrayList<LatLng> = arrayListOf(from)
        locationStopList.addAll(to)
        createMarkerRoute(locationStopList)
    }

    private fun createMarkerRoute(latLngList: List<LatLng>) {
        mapHelper.drawRoute(latLngList)
    }

    protected fun cleanMarkerRoute() {
        mapHelper.cleanRoute()
    }

    protected fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis() / 1000
    }

    protected fun setStatusSynced(status: Status, shouldUpdate: Boolean) {
        this.status = status
        currentRequest?.status = status

        if (shouldUpdate) FirebaseHelper.updateRequest(currentRequest!!)
    }

    protected abstract fun getActiveRequest()

    protected abstract fun getRequestInfo(requestId: String)

    protected abstract fun startRideRequest()

    protected abstract fun killRideRequest()

    protected abstract fun completeRideRequest()


    protected abstract fun showFreshUI()

    protected abstract fun showPendingUI()

    protected abstract fun showDriverToCustomerUI()

    protected abstract fun showToDestinationUI()

    protected abstract fun showPaymentUI()

    protected abstract fun showRatingUI()


    protected abstract fun showStatusUI()

    protected abstract fun switchState()

}
