package com.team.eddie.uber_alles.ui.generic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
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
import com.team.eddie.uber_alles.utils.map.MapRouteHelper
import java.util.*

class GenericRequestSingleFragment :
        Fragment(),
        OnMapReadyCallback {

    private lateinit var binding: FragmentGenericRequestSingleBinding

    private lateinit var mapFragment: SupportMapFragment
    private lateinit var mMap: GoogleMap
    private lateinit var mapHelper: MapRouteHelper

    private val currentUserId: String = FirebaseHelper.getUserId()

    private var requestId: String? = null

    private lateinit var requestInfoDb: DatabaseReference

    private lateinit var rideLocation: TextView
    private lateinit var rideDistance: TextView
    private lateinit var rideAmount: TextView
    private lateinit var rideDate: TextView
    private lateinit var name: TextView
    private lateinit var userPhone: TextView
    private lateinit var userImage: ImageView
    private lateinit var mRatingBar: RatingBar

    private var ridePrice: Double? = null

    private val destinationLatLngList: ArrayList<LatLng> = ArrayList()
    private var pickupLatLng: LatLng? = null


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
        rideAmount = binding.rideAmount
        rideDate = binding.rideDate
        name = binding.name
        userPhone = binding.userPhone

        userImage = binding.userImage

        mRatingBar = binding.ratingBar

        requestInfoDb = FirebaseHelper.getRequestKey(requestId!!)

        return binding.root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mapHelper = MapRouteHelper(mMap)

        getRideInformation()
    }

    protected fun getRouteToMarker(latLngList: List<LatLng>) {
        mapHelper.drawRoute(latLngList)
    }

    private fun getRideInformation() {
        requestInfoDb.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                    val request = dataSnapshot.getValue(Request::class.java)
                    request ?: return

                    if (currentUserId == request.driverId)
                        getUserInformation(request.customerId)

                    if (currentUserId == request.customerId) {
                        getUserInformation(request.driverId)
                        displayCustomerRelatedObjects(request.driverId)
                    }

                    rideDate.text = ActivityHelper.getDate(request.arrivingTime)

                    rideDistance.text = request.distance.toString() + " km"
                    rideAmount.text = request.amount.toString() + " €"

                    var tripStr: String = ""

                    request.pickupLocation?.let {
                        pickupLatLng = LatLng(it.latLng.latitude, it.latLng.longitude)
                        tripStr = request.pickupLocation!!.address
                    }
                    if (pickupLatLng == null) pickupLatLng = LatLng(0.0, 0.0)

                    for (place in request.destinationList) {
                        tripStr += " -> " + place.address
                        destinationLatLngList.add(LatLng(place.latLng.latitude, place.latLng.longitude))
                    }
                    rideLocation.text = tripStr

                    val locationStopList: ArrayList<LatLng> = arrayListOf(pickupLatLng!!)
                    locationStopList.addAll(destinationLatLngList)
                    getRouteToMarker(locationStopList)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun displayCustomerRelatedObjects(driverId: String) {
        mRatingBar.visibility = View.VISIBLE
        mRatingBar.onRatingBarChangeListener = RatingBar.OnRatingBarChangeListener { ratingBar, rating, fromUser ->

            requestInfoDb.child(RATING_LEG).setValue(rating)

            val mUserRatingDb = FirebaseHelper.getUserRating(driverId)
            mUserRatingDb.child(requestId!!).setValue(rating)
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

}
