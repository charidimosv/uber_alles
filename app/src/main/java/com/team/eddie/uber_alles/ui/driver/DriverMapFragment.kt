package com.team.eddie.uber_alles.ui.driver

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.databinding.FragmentDriverMapBinding
import com.team.eddie.uber_alles.ui.generic.GenericMapFragment
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper.addHistoryForDriverCustomer
import com.team.eddie.uber_alles.utils.SaveSharedPreference
import java.math.RoundingMode
import java.text.DecimalFormat


class DriverMapFragment : GenericMapFragment() {

    lateinit var binding: FragmentDriverMapBinding

    private var status = 0

    private var customerId = ""

    private var destination: String? = null
    private var destinationLatLng: LatLng? = null
    private var destinationMarker: Marker? = null

    private var pickupTime: Long? = null
    private var pickupLatLng: LatLng? = null
    private var rideDistance: Float = 0.toFloat()

    private lateinit var mCustomerInfo: LinearLayout

    private lateinit var mCustomerProfileImage: ImageView

    private lateinit var mCustomerName: TextView
    private lateinit var mCustomerPhone: TextView
    private lateinit var mCustomerDestination: TextView
    private lateinit var mRatingBar: RatingBar
    private lateinit var mRatingText: EditText
    private lateinit var mRatingButton: Button
    private lateinit var mRatingAvg: TextView

    private var newIncomeMessageRef: DatabaseReference? = null
    private var newIncomeMessageListener: ValueEventListener? = null

    private var pickupMarker: Marker? = null
    private var assignedCustomerPickupLocationRef: DatabaseReference? = null
    private var assignedCustomerPickupLocationRefListener: ValueEventListener? = null


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDriverMapBinding.inflate(inflater, container, false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations)
                    onLocationChanged(location)
            }
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.driver_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mCustomerInfo = binding.customerInfo
        mCustomerProfileImage = binding.customerProfileImage

        mCustomerName = binding.customerName
        mCustomerPhone = binding.customerPhone
        mCustomerDestination = binding.customerDestination

        mRatingBar = binding.ratingBar
        mRatingText = binding.ratingText
        mRatingButton = binding.ratingButton
        mRatingAvg = binding.ratingAvg


        binding.rideStatus.setOnClickListener {

            if (status == 1) {
                status = 2
                erasePolylines()
                if (destinationLatLng?.latitude != 0.0 && destinationLatLng?.longitude != 0.0) {
                    destinationMarker = mMap.addMarker(MarkerOptions().position(destinationLatLng!!).title("Destination").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)))
                    getRouteToMarker(destinationLatLng)
                }
                binding.rideStatus.text = "Drive completed"
                pickupTime = getCurrentTimestamp()

                val pickUpRef = FirebaseHelper.getDriverCustomerReqPickup(currentUserId)
                pickUpRef.setValue(true)

                binding.callCustomer.visibility = View.GONE
                binding.chatCustomer.visibility = View.GONE

            } else if (status == 2) {
                recordRide()
                endRide()
            }
        }

        mRatingButton.setOnClickListener {
            val ratingRef = FirebaseHelper.getUserRating(customerId)
            val ratingRefId = ratingRef.push().key

            val map = hashMapOf<String, Any?>("value" to mRatingBar.rating/*,"comment" to mRatingText*/)
            ratingRef.child(ratingRefId!!).updateChildren(map)

            clearCustomersInfo()
        }

        binding.chatCustomer.setOnClickListener {
            val direction = DriverMapFragmentDirections.actionDriverMapFragmentToChatFragment()
            it.findNavController().navigate(direction)
        }

        getAssignedCustomer()

        return binding.root
    }

    private fun getAssignedCustomer() {
        val assignedCustomerRef = FirebaseHelper.getDriverCustomerRide(currentUserId)
        assignedCustomerRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    status = 1
                    customerId = dataSnapshot.value!!.toString()
                    getAssignedCustomerPickupLocation()
                    getAssignedCustomerDestination()
                    getAssignedCustomerInfo()
                } else
                    endRide()

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun getAssignedCustomerPickupLocation() {
        assignedCustomerPickupLocationRef = FirebaseHelper.getCustomerRequestLocation(customerId)
        assignedCustomerPickupLocationRefListener = assignedCustomerPickupLocationRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && customerId != "") {
                    val map = dataSnapshot.value as List<Any?>

                    val locationLat: Double = map[0]?.toString()?.toDouble() ?: 0.0
                    val locationLng: Double = map[1]?.toString()?.toDouble() ?: 0.0

                    pickupLatLng = LatLng(locationLat, locationLng)
                    pickupMarker = mMap.addMarker(MarkerOptions().position(pickupLatLng!!).title("pickup location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)))
                    getRouteToMarker(pickupLatLng)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun getAssignedCustomerDestination() {
        val assignedCustomerRequestRef = FirebaseHelper.getDriverCustomerReq(currentUserId)

        assignedCustomerRequestRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val map = dataSnapshot.value as Map<String, Any>

                    destination = map["destination"]?.toString() ?: "--"
                    mCustomerDestination.text = "Destination: $destination"

                    val destinationLat = map["destinationLat"]?.toString()?.toDouble() ?: 0.0
                    val destinationLng = map["destinationLng"]?.toString()?.toDouble() ?: 0.0

                    destinationLatLng = LatLng(destinationLat, destinationLng)
                }

            }

            override fun onCancelled(p0: DatabaseError) {}
        })

    }

    private fun getAssignedCustomerInfo() {
        mCustomerInfo.visibility = View.VISIBLE
        val mCustomerDatabase = FirebaseHelper.getUserInfo(customerId)
        mCustomerDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                    val map = dataSnapshot.value as Map<String, Any>

                    map["name"]?.let { mCustomerName.text = it.toString() }
                    map["phone"]?.let { mCustomerPhone.text = it.toString() }
                    map["profileImageUrl"]?.let { Glide.with(activity?.application!!).load(it.toString()).into(mCustomerProfileImage) }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })

        val mCustomerRating = FirebaseHelper.getUserRating(customerId)
        mCustomerRating.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                //Load rating
                var ratingSum = 0.toFloat()
                var ratingsTotal = 0.toFloat()
                var ratingsAvg = 0.toFloat()
                val df = DecimalFormat("#.##")
                df.roundingMode = RoundingMode.CEILING
                for (rating in dataSnapshot.children) {
                    ratingSum += rating.child("value").value.toString().toFloat()
                    ratingsTotal++
                }
                if (ratingsTotal != 0.toFloat()) {
                    ratingsAvg = ratingSum / ratingsTotal
                    mRatingBar.rating = ratingsAvg
                }
                mRatingAvg.text = "Average Rating: " + df.format(ratingsAvg).toString() + "/5"

            }

        })

        SaveSharedPreference.setChatSender(activity!!.applicationContext, currentUserId)
        SaveSharedPreference.setChatReceiver(activity!!.applicationContext, customerId)
        newIncomeMessageRef = FirebaseHelper.getMessageUsers(currentUserId + "_to_" + customerId)

        binding.callCustomer.visibility = View.VISIBLE
        binding.chatCustomer.visibility = View.VISIBLE

        newIncomeMessageListener = newIncomeMessageRef?.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) binding.chatCustomer.text = "Message (!)"
            }

        })

    }

    private fun endRide() {
        binding.rideStatus.text = "Picked Customer"

        val driverRef = FirebaseHelper.getDriverCustomerReq(currentUserId)
        driverRef.removeValue()

        val ref = FirebaseHelper.getCustomerRequest()
        val geoFire = GeoFire(ref)
        geoFire.removeLocation(customerId)

        pickupMarker?.remove()
        destinationMarker?.remove()
        assignedCustomerPickupLocationRef?.removeEventListener(assignedCustomerPickupLocationRefListener!!)
        newIncomeMessageRef?.removeEventListener(newIncomeMessageListener!!)

        erasePolylines()

        if (status == 2) {
            mRatingBar.rating = 0.toFloat()
            mRatingBar.setIsIndicator(false)
            mRatingBar.numStars = 5
            mRatingAvg.visibility = View.GONE
            mRatingButton.visibility = View.VISIBLE
            mRatingText.visibility = View.VISIBLE
        } else
            clearCustomersInfo()

    }

    private fun clearCustomersInfo() {
        status = 0
        mRatingBar.rating = 0.toFloat()
        mRatingAvg.text = ""
        //mRatingText = null
        mRatingButton.visibility = View.GONE
        mRatingText.visibility = View.GONE

        customerId = ""
        mRatingBar.rating = 0.toFloat()
        mRatingBar.setIsIndicator(false)
        mRatingBar.numStars = 1
        mRatingAvg.text = ""
        mRatingAvg.visibility = View.VISIBLE
        mCustomerInfo.visibility = View.GONE
        mCustomerName.text = ""
        mCustomerPhone.text = ""
        mCustomerDestination.text = "Destination: --"
        mCustomerProfileImage.setImageResource(R.mipmap.ic_default_user)

    }

    private fun recordRide() {
        addHistoryForDriverCustomer(currentUserId, customerId, pickupTime, getCurrentTimestamp(), destination, rideDistance,
                pickupLatLng?.latitude, pickupLatLng?.longitude, destinationLatLng?.latitude, destinationLatLng?.longitude)
    }

    private fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis() / 1000
    }


    override fun onLocationChanged(location: Location?) {
        if (activity!!.applicationContext != null && location != null) {

            mLastLocation = location
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), DEFAULT_ZOOM))

            val refAvailable = FirebaseHelper.getDriversAvailable()
            val refWorking = FirebaseHelper.getDriversWorking()

            val geoFireAvailable = GeoFire(refAvailable)
            val geoFireWorking = GeoFire(refWorking)

            if (customerId.isBlank()) {
                geoFireWorking.removeLocation(currentUserId)
                geoFireAvailable.setLocation(currentUserId, GeoLocation(location.latitude, location.longitude))
            } else {
                geoFireAvailable.removeLocation(currentUserId)
                geoFireWorking.setLocation(currentUserId, GeoLocation(location.latitude, location.longitude))
            }
        }
    }

}
