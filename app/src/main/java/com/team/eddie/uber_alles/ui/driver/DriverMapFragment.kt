package com.team.eddie.uber_alles.ui.driver

import android.location.Location
import android.os.Bundle
import android.util.ArraySet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
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
import com.team.eddie.uber_alles.ui.ActivityHelper
import com.team.eddie.uber_alles.ui.generic.GenericMapFragment
import com.team.eddie.uber_alles.utils.SaveSharedPreference
import com.team.eddie.uber_alles.utils.Status
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import com.team.eddie.uber_alles.utils.firebase.Request
import com.team.eddie.uber_alles.utils.firebase.UserInfo
import java.math.RoundingMode
import java.text.DecimalFormat

private const val DEFAULT_SEARCH_RADIUS: Double = 10000.0

open class DriverMapFragment : GenericMapFragment() {

    /*
    ----------------------------------
    UI
    ----------------------------------
    */

    private lateinit var binding: FragmentDriverMapBinding

    /*
    ----------------------------------
    OTHER
    ----------------------------------
    */

    private var customerFoundId: String? = null
    private var customerFoundUsername: String? = null
    private var hasAccepted: Boolean = false

    private val destinationMarkerList: ArrayList<Marker> = ArrayList()
    private val destinationLatLngList: ArrayList<LatLng> = ArrayList()

    private var pickupTime: Long? = null
    private var pickupLatLng: LatLng? = null
    private var pickupMarker: Marker? = null

    private var rideDistance: Float = 0.toFloat()

    private var mCustomerMarker: Marker? = null
    private var customerLocationRef: DatabaseReference? = null
    private var customerLocationListener: ValueEventListener? = null

    private var pendingRequestQuery: GeoQuery? = null
    private var searchCustomersAround: Boolean = true
    private var rejectedCustomersSet = ArraySet<String>()


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDriverMapBinding.inflate(inflater, container, false)
        applicationContext = activity?.applicationContext!!

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

        popup = binding.popup

        destination = binding.destination
        userAllInfo = binding.userAllInfo

        userInfo = binding.userInfo
        userProfileImage = binding.userProfileImage
        userName = binding.userName
        userPhone = binding.userPhone

        currentRating = binding.currentRating
        currentRatingBar = binding.currentRatingBar
        currentRatingAvg = binding.currentRatingAvg

        newRating = binding.newRating
        newRatingBar = binding.newRatingBar
        newRatingText = binding.newRatingText
        newRatingButton = binding.newRatingButton

        communicateUser = binding.communicateUser

        callUser = binding.callUser
        chatUser = binding.chatUser

        rideStatus = binding.rideStatus


        chatUser.setOnClickListener {
            val direction = DriverMapFragmentDirections.actionDriverMapFragmentToChatFragment()
            it.findNavController().navigate(direction)
        }

        newRatingButton.setOnClickListener {
            val ratingRef = FirebaseHelper.getUserRating(customerFoundId!!)
            val ratingRefId = ratingRef.push().key

            val map = hashMapOf<String, Any?>("value" to newRatingBar.rating, "comment" to newRatingText.text.toString())
            ratingRef.child(ratingRefId!!).updateChildren(map)

            startFresh()
        }

        rideStatus.setOnClickListener { switchState() }

        getActiveRequest()

        return binding.root
    }

    override fun onLocationChanged(location: Location?) {
        super.onLocationChanged(location)

        if (customerFoundId == null && location != null)
            FirebaseHelper.addDriverAvailable(currentUserId, GeoLocation(location.latitude, location.longitude))
        else
            FirebaseHelper.removeDriverAvailable(currentUserId)
    }

    override fun getActiveRequest() {
        activeRequestRef = FirebaseHelper.getUserActiveRequest(currentUserId)
        activeRequestListener = activeRequestRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists())
                    getRequestInfo(dataSnapshot.value.toString())
                else
                    endRideRequest()
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    override fun getRequestInfo(requestId: String) {
        requestRef = FirebaseHelper.getRequestKey(requestId)
        requestListener = requestRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                    currentRequest = dataSnapshot.getValue(Request::class.java)
                    currentRequest ?: return

                    customerFoundId = currentRequest?.customerId
                    status = currentRequest?.status!!

                    var destinationAll: String = ""
                    for (loc in currentRequest?.destinationList!!)
                        destinationAll = loc.locName + " "
                    destination.text = destinationAll

                    showStatusUI()
                } else
                    endRideRequest()
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun getAssignedCustomerMessage() {
        val currentUser = SaveSharedPreference.getUserInfo(applicationContext)
        SaveSharedPreference.setChatSender(applicationContext, currentUser!!.username)
        SaveSharedPreference.setChatReceiver(applicationContext, customerFoundUsername!!)

        newIncomeMessageRef = FirebaseHelper.getMessageUsers(currentUser.username + "_to_" + customerFoundUsername!!)
        newIncomeMessageListener = newIncomeMessageRef?.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) chatUser.text = getString(R.string.message_excl)
            }

        })
    }

    private fun getAssignedCustomerInfo() {
        val mCustomerDatabase = FirebaseHelper.getUserInfo(customerFoundId!!)
        mCustomerDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                    val userInfo = dataSnapshot.getValue(UserInfo::class.java)
                    userInfo ?: return

                    userInfo.name.let { userName.text = it }
                    userInfo.username.let { customerFoundUsername = it }
                    userInfo.phone.let { userPhone.text = it }
                    userInfo.imageUrl?.let { ActivityHelper.bindImageFromUrl(userProfileImage, it) }

                    if (showMessages) getAssignedCustomerMessage()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })

        val mCustomerRating = FirebaseHelper.getUserRating(customerFoundId!!)
        mCustomerRating.addListenerForSingleValueEvent(
                object : ValueEventListener {
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
                            currentRatingBar.rating = ratingsAvg
                        }
                        currentRatingAvg.text = "Average Rating: " + df.format(ratingsAvg).toString() + "/5"

                    }

                })
    }

    private fun getAssignedCustomerLocation() {
        customerLocationRef = FirebaseHelper.getUserLocation(customerFoundId!!)
        customerLocationListener = customerLocationRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val map = dataSnapshot.value as List<Any?>

                    val locationLat: Double = map[0]?.toString()?.toDouble() ?: 0.0
                    val locationLng: Double = map[1]?.toString()?.toDouble() ?: 0.0

                    val latLng = LatLng(locationLat, locationLng)

                    val loc1 = Location("")
                    loc1.latitude = mLastLocation!!.latitude
                    loc1.longitude = mLastLocation!!.longitude

                    val loc2 = Location("")
                    loc2.latitude = latLng.latitude
                    loc2.longitude = latLng.longitude

                    val distance: Float = loc1.distanceTo(loc2)

                    binding.rideStatus.text = if (distance < 100) getString(R.string.picked_customer) else getString(R.string.distance).plus(distance.toString())

                    mCustomerMarker?.remove()
                    mCustomerMarker = mMap.addMarker(MarkerOptions().position(latLng).title(getString(R.string.your_driver)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_default_user)))
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun findNextCustomer() {
        val geoPendingRequest = GeoFire(FirebaseHelper.getPendingRequest())
        pendingRequestQuery = mLastLocation?.let { geoPendingRequest.queryAtLocation(GeoLocation(it.latitude, it.longitude), DEFAULT_SEARCH_RADIUS) }

        pendingRequestQuery?.removeAllListeners()
        pendingRequestQuery?.addGeoQueryEventListener(object : GeoQueryEventListener {
            override fun onKeyEntered(key: String, location: GeoLocation) {
                if (customerFoundId == null
                        && !rejectedCustomersSet.contains(key)
                        && searchCustomersAround) {
                    searchCustomersAround = false
                    getRequestInfo(key)
                }
            }

            override fun onKeyExited(key: String) {
                if (!hasAccepted) startFresh()
            }

            override fun onKeyMoved(key: String, location: GeoLocation) {}

            override fun onGeoQueryReady() {}

            override fun onGeoQueryError(error: DatabaseError) {}
        })
    }

    private fun acceptCustomerRequest() {
        currentRequest ?: return

        hasAccepted = true
        status = Status.DriverToCustomer
        currentRequest?.status = status
        currentRequest?.driverId = currentUserId
        FirebaseHelper.acceptRequest(currentRequest!!)

        showDriverToCustomerUI()
    }

    override fun startRideRequest() {
    }

    override fun endRideRequest() {
        requestListener?.let { requestRef?.removeEventListener(it) }
        if (completedRide) return

        currentRequest?.let { FirebaseHelper.removeRequest(it) }
        startFresh()
    }

    private fun recordRide() {
        // TODO - hackie-hackie-hackie
        val listSize = currentRequest?.destinationList?.size ?: 0
        val lastRequestLocation = currentRequest?.destinationList?.get(listSize - 1)
        FirebaseHelper.addHistoryForDriverCustomer(currentUserId, customerFoundId!!, pickupTime, getCurrentTimestamp(), lastRequestLocation?.locName, rideDistance,
                pickupLatLng?.latitude, pickupLatLng?.longitude, lastRequestLocation?.lat, lastRequestLocation?.lng)

        currentRequest ?: return

        currentRequest?.arrivingTime = getCurrentTimestamp()
        currentRequest?.distance = rideDistance
        currentRequest?.pickupTime = pickupTime!!

        FirebaseHelper.completeRequest(currentRequest!!)
        requestListener?.let { requestRef?.removeEventListener(it) }
    }

    private fun syncRequestDestination() {
        for (reqLocation in currentRequest!!.destinationList!!)
            destinationLatLngList.add(LatLng(reqLocation.lat, reqLocation.lng))

        if (!destinationLatLngList.isEmpty()) {
            for (latLng in destinationLatLngList) {
                val destinationMarker = mMap.addMarker(MarkerOptions()
                        .position(latLng)
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)))
                destinationMarkerList.add(destinationMarker)
            }
        }
    }

    private fun clearDestinationInfo() {
        for (marker in destinationMarkerList) marker.remove()
        destinationMarkerList.clear()
        destinationLatLngList.clear()
    }

    private fun clearCustomersInfo() {
        currentRequest = null
        customerFoundId = null
        customerFoundUsername = null

        /*
        ----------------------------------
        UI
        ----------------------------------
        */
        popup.visibility = View.GONE

        destination.text = ""

        userInfo.visibility = View.GONE

        userProfileImage.setImageResource(R.mipmap.ic_default_user)
        userName.text = ""
        userPhone.text = ""

        currentRating.visibility = View.GONE
        currentRatingBar.rating = 0.toFloat()
        currentRatingAvg.text = ""

        newRating.visibility = View.GONE
        newRatingBar.rating = 0.toFloat()
        newRatingText.setText("")

        communicateUser.visibility = View.GONE

        rideStatus.visibility = View.GONE
        rideStatus.isClickable = true
        rideStatus.text = getString(R.string.accept_customer)
        /*
        ----------------------------------
        UI
        ----------------------------------
        */
    }

    private fun startFresh() {
        showFreshUI()
        findNextCustomer()
    }

    override fun showFreshUI() {
        status = Status.Free

        searchCustomersAround = true
        completedRide = false
        showMessages = false
        hasAccepted = false

        pickupMarker?.remove()
        mCustomerMarker?.remove()

        erasePolylines()
        clearDestinationInfo()
        clearCustomersInfo()

        requestListener?.let { requestRef?.removeEventListener(it) }
        customerLocationListener?.let { customerLocationRef?.removeEventListener(it) }
        newIncomeMessageListener?.let { newIncomeMessageRef?.removeEventListener(it) }
    }

    override fun showPendingUI() {
        status = Status.Pending

        /*
        ----------------------------------
        UI
        ----------------------------------
        */
        popup.visibility = View.VISIBLE

        destination.text = ""

        userInfo.visibility = View.VISIBLE

        userProfileImage.setImageResource(R.mipmap.ic_default_user)
        userName.text = ""
        userPhone.text = ""

        currentRating.visibility = View.VISIBLE
        currentRatingBar.rating = 0.toFloat()
        currentRatingAvg.text = ""

        newRating.visibility = View.GONE
        newRatingBar.rating = 0.toFloat()
        newRatingText.setText("")

        communicateUser.visibility = View.GONE

        rideStatus.visibility = View.VISIBLE
        rideStatus.isClickable = true
        rideStatus.text = getString(R.string.accept_customer)
        /*
        ----------------------------------
        UI
        ----------------------------------
        */

        searchCustomersAround = false
        completedRide = false
        showMessages = false

        pickupLatLng = LatLng(currentRequest!!.pickupLocation!!.lat, currentRequest!!.pickupLocation!!.lng)
        pickupMarker?.remove()
        pickupMarker = mMap.addMarker(MarkerOptions().position(pickupLatLng!!).title(getString(R.string.pickup_here)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)))

        erasePolylines()
        clearDestinationInfo()
        syncRequestDestination()

        val locationStopList: ArrayList<LatLng> = arrayListOf(LatLng(mLastLocation!!.latitude, mLastLocation!!.longitude), pickupLatLng!!)
        locationStopList.addAll(destinationLatLngList)
        getRouteToMarker(locationStopList)

        customerLocationListener?.let { customerLocationRef?.removeEventListener(it) }
        newIncomeMessageListener?.let { newIncomeMessageRef?.removeEventListener(it) }

        getAssignedCustomerInfo()
//        getAssignedCustomerLocation()
    }

    override fun showDriverToCustomerUI() {
        status = Status.DriverToCustomer

        /*
        ----------------------------------
        UI
        ----------------------------------
        */
        popup.visibility = View.VISIBLE

        destination.text = ""

        userInfo.visibility = View.VISIBLE

        userProfileImage.setImageResource(R.mipmap.ic_default_user)
        userName.text = ""
        userPhone.text = ""

        currentRating.visibility = View.VISIBLE
        currentRatingBar.rating = 0.toFloat()
        currentRatingAvg.text = ""

        newRating.visibility = View.GONE
        newRatingBar.rating = 0.toFloat()
        newRatingText.setText("")

        communicateUser.visibility = View.VISIBLE

        rideStatus.visibility = View.VISIBLE
        rideStatus.isClickable = true
        rideStatus.text = getString(R.string.picked_customer)
        /*
        ----------------------------------
        UI
        ----------------------------------
        */

        searchCustomersAround = false
        completedRide = false
        showMessages = true

        pickupLatLng = LatLng(currentRequest!!.pickupLocation!!.lat, currentRequest!!.pickupLocation!!.lng)
        pickupMarker?.remove()
        pickupMarker = mMap.addMarker(MarkerOptions().position(pickupLatLng!!).title(getString(R.string.pickup_here)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)))

        erasePolylines()
        clearDestinationInfo()
        getRouteToMarker(pickupLatLng)

        customerLocationListener?.let { customerLocationRef?.removeEventListener(it) }
        newIncomeMessageListener?.let { newIncomeMessageRef?.removeEventListener(it) }

        getAssignedCustomerInfo()
        getAssignedCustomerLocation()
    }

    override fun showRideUI() {
        status = Status.ToDestination

        /*
        ----------------------------------
        UI
        ----------------------------------
        */
        popup.visibility = View.GONE

        destination.text = ""

        userInfo.visibility = View.GONE

        userProfileImage.setImageResource(R.mipmap.ic_default_user)
        userName.text = ""
        userPhone.text = ""

        currentRating.visibility = View.GONE
        currentRatingBar.rating = 0.toFloat()
        currentRatingAvg.text = ""

        newRating.visibility = View.GONE
        newRatingBar.rating = 0.toFloat()
        newRatingText.setText("")

        communicateUser.visibility = View.GONE

        rideStatus.visibility = View.VISIBLE
        rideStatus.isClickable = true
        rideStatus.text = getString(R.string.drive_completed)
        /*
        ----------------------------------
        UI
        ----------------------------------
        */

        searchCustomersAround = false
        completedRide = false
        showMessages = false

        pickupTime = getCurrentTimestamp()

        pickupMarker?.remove()
        mCustomerMarker?.remove()

        erasePolylines()
        clearDestinationInfo()
        syncRequestDestination()
        getRouteToMarker(LatLng(mLastLocation!!.latitude, mLastLocation!!.longitude), destinationLatLngList)

        customerLocationListener?.let { customerLocationRef?.removeEventListener(it) }
        newIncomeMessageListener?.let { newIncomeMessageRef?.removeEventListener(it) }
    }

    override fun showRatingUI() {
        status = Status.Rating

        /*
        ----------------------------------
        UI
        ----------------------------------
        */
        popup.visibility = View.VISIBLE

        destination.text = ""

        userInfo.visibility = View.VISIBLE

        userProfileImage.setImageResource(R.mipmap.ic_default_user)
        userName.text = ""
        userPhone.text = ""

        currentRating.visibility = View.GONE
        currentRatingBar.rating = 0.toFloat()
        currentRatingAvg.text = ""

        newRating.visibility = View.VISIBLE
        newRatingBar.rating = 0.toFloat()
        newRatingText.setText("")

        communicateUser.visibility = View.GONE

        rideStatus.visibility = View.VISIBLE
        rideStatus.isClickable = true
        rideStatus.text = getString(R.string.cancel)
        /*
        ----------------------------------
        UI
        ----------------------------------
        */

        showMessages = false

        erasePolylines()
        clearDestinationInfo()

        requestListener?.let { requestRef?.removeEventListener(it) }
        customerLocationListener?.let { customerLocationRef?.removeEventListener(it) }
        newIncomeMessageListener?.let { newIncomeMessageRef?.removeEventListener(it) }
    }

    override fun showStatusUI() {
        when (status) {
            Status.Free -> {
            }
            Status.Pending -> showPendingUI()
            Status.DriverToCustomer -> showDriverToCustomerUI()
            Status.ToDestination -> showRideUI()
            Status.Payment -> {
            }
            Status.Rating -> showRatingUI()
            Status.Done -> {
            }
        }
    }

    override fun switchState() {
        when (status) {
            Status.Free -> {
            }
            Status.Pending -> {
                acceptCustomerRequest()
            }
            Status.DriverToCustomer -> {
                setStatusSynced(Status.ToDestination)
                showRideUI()
            }
            Status.ToDestination -> {
                if (currentRequest != null) {
                    completedRide = true
                    recordRide()
                    showRatingUI()
                }
            }
            Status.Payment -> {
            }
            Status.Rating -> {
                endRideRequest()
            }
            Status.Done -> {
            }
        }
    }

}
