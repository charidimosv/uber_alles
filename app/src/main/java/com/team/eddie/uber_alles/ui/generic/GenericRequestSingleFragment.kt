package com.team.eddie.uber_alles.ui.generic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.directions.route.Route
import com.directions.route.RouteException
import com.directions.route.RoutingListener
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.databinding.FragmentGenericRequestSingleBinding
import com.team.eddie.uber_alles.ui.ActivityHelper
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper.RATING_LEG
import com.team.eddie.uber_alles.utils.firebase.Request
import com.team.eddie.uber_alles.utils.firebase.UserInfo
import java.util.*

class GenericRequestSingleFragment :
        Fragment(),
        OnMapReadyCallback,
        RoutingListener {

    private lateinit var binding: FragmentGenericRequestSingleBinding

    private lateinit var mMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment

    private val currentUserId: String = FirebaseHelper.getUserId()

    private var requestId: String? = null

    private lateinit var requestInfoDb: DatabaseReference

    private lateinit var rideLocation: TextView
    private lateinit var rideDistance: TextView
    private lateinit var rideDate: TextView
    private lateinit var name: TextView
    private lateinit var userPhone: TextView
    private lateinit var userImage: ImageView
    private lateinit var mRatingBar: RatingBar

    private var destinationLatLng: LatLng? = null
    private var pickupLatLng: LatLng? = null
    private var distance: String? = null
    private var ridePrice: Double? = null
    private var customerPaid: Boolean? = false

    private var polylines: MutableList<Polyline> = ArrayList()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGenericRequestSingleBinding.inflate(inflater, container, false)
        requestId = GenericRequestSingleFragmentArgs.fromBundle(arguments).requestId

        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        rideLocation = binding.rideLocation
        rideDistance = binding.rideDistance
        rideDate = binding.rideDate
        name = binding.name
        userPhone = binding.userPhone

        userImage = binding.userImage

        mRatingBar = binding.ratingBar

        requestInfoDb = FirebaseHelper.getRequestKey(requestId!!)
        getRideInformation()

        return binding.root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    private fun getRideInformation() {
        requestInfoDb.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                    val request = dataSnapshot.getValue(Request::class.java)

                    request?.let {
                        it.requestId = dataSnapshot.key!!

                        if (currentUserId == it.driverId)
                            it.customerId?.let { customerId -> getUserInformation(customerId) }

                        if (currentUserId == it.customerId) {
                            it.driverId?.let { driverId ->
                                getUserInformation(driverId)
                                displayCustomerRelatedObjects(driverId)
                            }
                        }

                        rideDate.text = ActivityHelper.getDate(it.arrivingTime)

                        distance = it.distance?.toString()
                        if (distance != null) {
                            rideDistance.text = distance!!.substring(0, Math.min(distance!!.length, 5)) + " km"
                            ridePrice = java.lang.Double.valueOf(distance!!) * 0.5
                        }

                        var destinationAll: String = ""
                        for (loc in it.destinationList!!)
                            destinationAll = loc.locName + " "
                        rideLocation.text = destinationAll

                        pickupLatLng = it.pickupLocation?.lat?.let { it1 -> it.pickupLocation?.lng?.let { it2 -> LatLng(it1, it2) } }
                        val destListSize = it.destinationList?.size ?: 0
                        if (destListSize > 0) {
                            destinationLatLng = it.destinationList?.get(destListSize - 1)?.lat?.let { it1 -> it.destinationList?.get(destListSize - 1)?.lat?.let { it2 -> LatLng(it1, it2) } }
                            if (destinationLatLng !== LatLng(0.0, 0.0)) getRouteToMarker()
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun displayCustomerRelatedObjects(driverId: String) {
        mRatingBar.visibility = View.VISIBLE
        mRatingBar.onRatingBarChangeListener = RatingBar.OnRatingBarChangeListener { ratingBar, rating, fromUser ->

            requestInfoDb.child(RATING_LEG).setValue(rating)

            val mDriverRatingDb = FirebaseHelper.getUserRating(driverId)
            mDriverRatingDb.child(requestId!!).setValue(rating)
        }
    }

    private fun getUserInformation(otherUserId: String) {
        val mOtherUserDB = FirebaseHelper.getUserInfo(otherUserId)
        mOtherUserDB.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                    val userInfo = dataSnapshot.getValue(UserInfo::class.java)
                    userInfo ?: return

                    userInfo.name.let { name.text = it }
                    userInfo.phone.let { userPhone.text = it }
                    userInfo.imageUrl?.let { ActivityHelper.bindImageFromUrl(userImage, it) }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun getRouteToMarker() {
        if (pickupLatLng != null && destinationLatLng != null)
            ActivityHelper.getRouteToMarker(arrayListOf(pickupLatLng!!, destinationLatLng!!), this)
    }

    override fun onRoutingFailure(e: RouteException?) {
        if (e != null) Toast.makeText(activity!!.applicationContext, "Error: " + e.message, Toast.LENGTH_LONG).show()
        else Toast.makeText(activity!!.applicationContext, "Something went wrong, Try again", Toast.LENGTH_SHORT).show()
    }

    override fun onRoutingStart() {}

    override fun onRoutingSuccess(route: ArrayList<Route>, shortestRouteIndex: Int) {

        val builder = LatLngBounds.Builder()
        builder.include(pickupLatLng!!)
        builder.include(destinationLatLng!!)
        val bounds = builder.build()

        val width = resources.displayMetrics.widthPixels
        val padding = (width * 0.2).toInt()

        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)

        mMap.animateCamera(cameraUpdate)

        mMap.addMarker(MarkerOptions().position(pickupLatLng!!).title("pickup location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)))
        mMap.addMarker(MarkerOptions().position(destinationLatLng!!).title("destination"))

        if (polylines.size > 0) for (poly in polylines) poly.remove()

        polylines = ArrayList()
        //add route(s) to the map.
        for (i in route.indices) {

            //In case of more than 5 alternative routes
            val colorIndex = i % COLORS.size

            val polyOptions = PolylineOptions()
            polyOptions.color(resources.getColor(COLORS[colorIndex]))
            polyOptions.width((10 + i * 3).toFloat())
            polyOptions.addAll(route[i].points)
            val polyline = mMap.addPolyline(polyOptions)
            polylines.add(polyline)


//            Toast.makeText(activity!!.applicationContext, "Route " + (i + 1) + ": distance - " + route[i].distanceValue + ": duration - " + route[i].durationValue, Toast.LENGTH_SHORT).show()
        }

    }

    override fun onRoutingCancelled() {}

    companion object {
        private val COLORS = intArrayOf(R.color.primary_dark_material_light)
    }

}