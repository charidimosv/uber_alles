package com.team.eddie.uber_alles.ui.generic

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.directions.route.*
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
import com.team.eddie.uber_alles.databinding.FragmentGenericHistorySingleBinding
import com.team.eddie.uber_alles.ui.ActivityHelper
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper.RATING_LEG
import com.team.eddie.uber_alles.utils.firebase.HistoryItem
import com.team.eddie.uber_alles.utils.firebase.UserInfo
import java.util.*

class GenericHistorySingleFragment :
        Fragment(),
        OnMapReadyCallback,
        RoutingListener {

    private lateinit var binding: FragmentGenericHistorySingleBinding

    private lateinit var mMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment

    private val currentUserId: String = FirebaseHelper.getUserId()

    private var rideId: String? = null

    private lateinit var historyRideInfoDb: DatabaseReference

    private lateinit var rideLocation: TextView
    private lateinit var rideDistance: TextView
    private lateinit var rideDate: TextView
    private lateinit var userName: TextView
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
        binding = FragmentGenericHistorySingleBinding.inflate(inflater, container, false)
        rideId = GenericHistorySingleFragmentArgs.fromBundle(arguments).rideId

        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        rideLocation = binding.rideLocation
        rideDistance = binding.rideDistance
        rideDate = binding.rideDate
        userName = binding.userName
        userPhone = binding.userPhone

        userImage = binding.userImage

        mRatingBar = binding.ratingBar

        historyRideInfoDb = FirebaseHelper.getHistoryKey(rideId!!)
        getRideInformation()

        return binding.root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    private fun getRideInformation() {
        historyRideInfoDb.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                    val historyItem = dataSnapshot.getValue(HistoryItem::class.java)

                    historyItem?.let {
                        it.rideId = dataSnapshot.key

                        if (currentUserId == it.driver)
                            it.customer?.let { customerId -> getUserInformation(customerId) }

                        if (currentUserId == it.customer) {
                            it.driver?.let { driverId ->
                                getUserInformation(driverId)
                                displayCustomerRelatedObjects(driverId)
                            }
                        }

                        rideDate.text = getDate(it.arrivingTime)

                        distance = it.distance?.toString()
                        if (distance != null) {
                            rideDistance.text = distance!!.substring(0, Math.min(distance!!.length, 5)) + " km"
                            ridePrice = java.lang.Double.valueOf(distance!!) * 0.5
                        }

                        rideLocation.text = it.destination

                        pickupLatLng = it.location?.from?.lat?.let { it1 -> it.location?.from?.lng?.let { it2 -> LatLng(it1, it2) } }
                        destinationLatLng = it.location?.to?.lat?.let { it1 -> it.location?.to?.lng?.let { it2 -> LatLng(it1, it2) } }
                        if (destinationLatLng !== LatLng(0.0, 0.0)) getRouteToMarker()
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun displayCustomerRelatedObjects(driverId: String) {
        mRatingBar.visibility = View.VISIBLE
        mRatingBar.onRatingBarChangeListener = RatingBar.OnRatingBarChangeListener { ratingBar, rating, fromUser ->

            historyRideInfoDb.child(RATING_LEG).setValue(rating)

            val mDriverRatingDb = FirebaseHelper.getUserRating(driverId)
            mDriverRatingDb.child(rideId!!).setValue(rating)
        }
    }

    private fun getUserInformation(otherUserId: String) {
        val mOtherUserDB = FirebaseHelper.getUserInfo(otherUserId)
        mOtherUserDB.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                    val userInfo = dataSnapshot.getValue(UserInfo::class.java)
                    userInfo ?: return

                    userInfo.name?.let { userName.text = it }
                    userInfo.phone.let { userPhone.text = it }
                    userInfo.imageUrl?.let { ActivityHelper.bindImageFromUrl(userImage, it) }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun getDate(time: Long?): String {
        val cal = Calendar.getInstance(Locale.getDefault())
        cal.timeInMillis = time!! * 1000
        return DateFormat.format("MM-dd-yyyy hh:mm", cal).toString()
    }

    private fun getRouteToMarker() {
        val routing = Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(pickupLatLng, destinationLatLng)
                .build()
        routing.execute()
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
