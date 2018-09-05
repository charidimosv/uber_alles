package com.team.eddie.uber_alles.ui.driver

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
import com.team.eddie.uber_alles.utils.FirebaseHelper
import com.team.eddie.uber_alles.utils.FirebaseHelper.addHistoryForDriverCustomer


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

    private var isLoggingOut: Boolean = false

    private var mCustomerInfo: LinearLayout? = null

    private var mCustomerProfileImage: ImageView? = null

    private var mCustomerName: TextView? = null
    private var mCustomerPhone: TextView? = null
    private var mCustomerDestination: TextView? = null
    private var mRatingBar: RatingBar? = null
    private var mRatingText: EditText? = null
    private var mRatingButton: Button? = null


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

        binding.rideStatus.setOnClickListener {

            if (status == 1) {
                status = 2
                erasePolylines()
                if (destinationLatLng?.latitude != 0.0 && destinationLatLng?.longitude != 0.0) {
                    destinationMarker = mMap.addMarker(MarkerOptions().position(destinationLatLng!!).title("Destination").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)))
                    getRouteToMarker(destinationLatLng);
                }
                binding.rideStatus.text = "Drive completed"
                pickupTime = getCurrentTimestamp()

                val pickUpRef = FirebaseHelper.getDriverCustomerReqPickup(currentUserId)
                pickUpRef.setValue(true)

            } else if (status == 2) {
                recordRide()
                endRide()
            }
        }

        mRatingBar = binding.ratingBar
        mRatingText = binding.ratingText
        mRatingButton = binding.ratingButton

        mRatingButton!!.setOnClickListener {

            val ratingRef = FirebaseHelper.getCustomerRating(customerId)
            val ratingRefId = ratingRef.push().key

            val map = hashMapOf<String, Any?>("value" to mRatingBar!!.rating/*,"comment" to mRatingText*/)

            ratingRef.child(ratingRefId!!).updateChildren(map)

            clearCustomersInfo()
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.driver_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mCustomerInfo = binding.customerInfo

        mCustomerProfileImage = binding.customerProfileImage

        mCustomerName = binding.customerName
        mCustomerPhone = binding.customerPhone
        mCustomerDestination = binding.customerDestination


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

    private var pickupMarker: Marker? = null
    private var assignedCustomerPickupLocationRef: DatabaseReference? = null
    private var assignedCustomerPickupLocationRefListener: ValueEventListener? = null
    private fun getAssignedCustomerPickupLocation() {
        assignedCustomerPickupLocationRef = FirebaseHelper.getCustomerRequestLocation(customerId)
        assignedCustomerPickupLocationRefListener = assignedCustomerPickupLocationRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && customerId != "") {
                    val map: List<*> = dataSnapshot.value as List<*>

                    val locationLat: Double = if (map[0] == null) 0.0 else map[0].toString().toDouble()
                    val locationLng: Double = if (map[1] == null) 0.0 else map[1].toString().toDouble()

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
                    val map: Map<String, Any> = dataSnapshot.value as Map<String, Any>

                    if (map["destination"] != null) {
                        destination = map["destination"].toString()
                        mCustomerDestination?.text = "Destination: $destination"
                    } else
                        mCustomerDestination?.text = "Destination: --"

                    val destinationLat: Double = if (map["destinationLat"] == null) 0.0 else map["destinationLat"].toString().toDouble()
                    val destinationLng: Double = if (map["destinationLng"] == null) 0.0 else map["destinationLng"].toString().toDouble()

                    destinationLatLng = LatLng(destinationLat, destinationLng)

                }

            }

            override fun onCancelled(p0: DatabaseError) {}
        })

    }


    private fun getAssignedCustomerInfo() {
        mCustomerInfo?.visibility = View.VISIBLE
        val mCustomerDatabase = FirebaseHelper.getCustomer(customerId)
        mCustomerDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                    val map: Map<String, Any> = dataSnapshot.value as Map<String, Any>

                    if (map["name"] != null)
                        mCustomerName?.text = map["name"].toString()

                    if (map["phone"] != null)
                        mCustomerPhone?.text = map["phone"].toString()

                    if (map["profileImageUrl"] != null)
                        Glide.with(mCustomerInfo!!).load(map["profileImageUrl"].toString()).into(mCustomerProfileImage!!)

                    //Load rating
                    var ratingSum = 0.toFloat()
                    var ratingsTotal = 0.toFloat()
                    var ratingsAvg = 0.toFloat()
                    for (rating in dataSnapshot.child("rating").children) {
                        ratingSum += rating.child("value").value.toString().toFloat()
                        ratingsTotal++
                    }
                    if (ratingsTotal != 0.toFloat()) {
                        ratingsAvg = ratingSum / ratingsTotal
                        mRatingBar?.rating = ratingsAvg
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun endRide() {
        binding.rideStatus.text = "Picked Customer"

        val driverRef = FirebaseHelper.getDriverCustomerReq(currentUserId)
        driverRef.removeValue();

        val ref = FirebaseHelper.getCustomerRequest()
        val geoFire = GeoFire(ref)
        geoFire.removeLocation(customerId)

        pickupMarker?.remove()
        destinationMarker?.remove()
        assignedCustomerPickupLocationRef?.removeEventListener(assignedCustomerPickupLocationRefListener!!)

        erasePolylines()

        if (status == 2) {
            mRatingBar?.rating = 0.toFloat()
            mRatingButton?.visibility = View.VISIBLE
            mRatingText?.visibility = View.VISIBLE
        } else
            clearCustomersInfo()

    }

    private fun clearCustomersInfo() {
        status = 0
        mRatingBar?.rating = 0.toFloat()
        //mRatingText = null
        mRatingButton?.visibility = View.GONE
        mRatingText?.visibility = View.GONE

        customerId = ""
        rideDistance = 0.toFloat()
        mCustomerInfo?.visibility = View.GONE
        mCustomerName?.text = ""
        mCustomerPhone?.text = ""
        mCustomerDestination?.text = "Destination: --"
        mCustomerProfileImage?.setImageResource(R.mipmap.ic_default_user)

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

    private fun disconnectDriver() {
        val refAvailable = FirebaseHelper.getDriversAvailable()
        val refWorking = FirebaseHelper.getDriversWorking()
        val geoFireAvailable = GeoFire(refAvailable)
        val geoFireWorking = GeoFire(refWorking)

        geoFireAvailable.removeLocation(currentUserId)
    }

    override fun onStop() {
        super.onStop()
        if (!isLoggingOut)
            disconnectDriver()
    }


}
