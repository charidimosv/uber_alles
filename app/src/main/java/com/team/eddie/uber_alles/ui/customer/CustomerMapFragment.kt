package com.team.eddie.uber_alles.ui.customer

import android.app.DatePickerDialog
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.navigation.findNavController
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.ui.PlaceSelectionListener
import com.google.android.gms.location.places.ui.SupportPlaceAutocompleteFragment
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.databinding.FragmentCustomerMapBinding
import com.team.eddie.uber_alles.ui.ActivityHelper
import com.team.eddie.uber_alles.ui.generic.GenericMapFragment
import com.team.eddie.uber_alles.utils.SaveSharedPreference
import com.team.eddie.uber_alles.utils.Status
import com.team.eddie.uber_alles.utils.firebase.Car
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import com.team.eddie.uber_alles.utils.firebase.Request
import com.team.eddie.uber_alles.utils.firebase.UserInfo
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

private const val DEFAULT_SEARCH_RADIUS: Double = 5555000.0
private const val DEFAULT_SEARCH_LOC_DIF: Float = 1F

class CustomerMapFragment : GenericMapFragment() {

    /*
    ----------------------------------
    UI
    ----------------------------------
    */

    private lateinit var binding: FragmentCustomerMapBinding

    private lateinit var searchRequest: LinearLayout
    private lateinit var autocompleteFragment: SupportPlaceAutocompleteFragment

    private lateinit var paymentInfo: LinearLayout
    private lateinit var payment: MaterialButton

    private lateinit var carInfo: LinearLayout
    private lateinit var carImage: ImageView
    private lateinit var carBrand: TextView
    private lateinit var carModel: TextView
    private lateinit var carPlate: TextView

    /*
    ----------------------------------
    OTHER
    ----------------------------------
    */

    private var driverFoundID: String? = null
    private var driverFoundUsername: String? = null

    private var dateOfRide: String? = null
    private val destinationMap: HashMap<Marker, Place> = HashMap()
    private val destinationList: ArrayList<Place> = ArrayList()

    private var isPickedUp: Boolean = false
    private var pickupLatLng: LatLng? = null
    private var pickupMarker: Marker? = null

    private var mDriverMarker: Marker? = null
    private var driverLocationRef: DatabaseReference? = null
    private var driverLocationListener: ValueEventListener? = null


    /*
    ----------------------------------
    DRIVERS AROUND
    ----------------------------------
    */

    private var showDriversAround: Boolean = true
    private var driversAroundLatestLoc: Location? = null
    private var driversAroundQuery: GeoQuery? = null
    private var driversAroundMarkerMap = mutableMapOf<String, Marker>()


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCustomerMapBinding.inflate(inflater, container, false)
        applicationContext = activity?.applicationContext!!

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations)
                    onLocationChanged(location)
            }
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.customer_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        searchRequest = binding.searchRequest

        popup = binding.popup

        destination = binding.destination
        userAllInfo = binding.userAllInfo

        userInfo = binding.userInfo
        userProfileImage = binding.userProfileImage
        userName = binding.userName
        userPhone = binding.userPhone

        carInfo = binding.carInfo
        carImage = binding.carImage
        carBrand = binding.carBrand
        carModel = binding.carModel
        carPlate = binding.carPlate

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

        paymentInfo = binding.paymentInfo
        payment = binding.payment

        rideStatus = binding.rideStatus


        /*
        ----------------------------------
        AutoCompleteFragment
        ----------------------------------
        */

        autocompleteFragment = childFragmentManager.findFragmentById(R.id.place_autocomplete_fragment) as SupportPlaceAutocompleteFragment
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                val destination = place.name.toString()
                val destinationMarker = mMap.addMarker(MarkerOptions()
                        .position(place.latLng)
                        .title(destination)
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)))
                destinationMap[destinationMarker] = place
                destinationList.add(place)

                followMeFlag = false
                moveCamera(place.latLng)

                mLastLocation?.let { createMarkerRoute(LatLng(it.latitude, it.longitude), getLatLngList()) }

                if (dateOfRide != null) rideStatus.visibility = View.VISIBLE
            }

            override fun onError(p0: com.google.android.gms.common.api.Status?) {}
        })

        /*
        ----------------------------------
        DatePicker
        ----------------------------------
        */

        val datePickerListener = DatePickerDialog.OnDateSetListener { view: DatePicker?, year: Int, month: Int, dayOfMonth: Int ->
            val newCalendar = Calendar.getInstance()
            newCalendar.set(year, month, dayOfMonth)
            dateOfRide = SimpleDateFormat("dd/MM/yyy", Locale("el")).format(newCalendar.time)
            binding.rideDate.text = dateOfRide

            if (!destinationList.isEmpty())
                rideStatus.visibility = View.VISIBLE

        }
        binding.rideDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(activity!!, datePickerListener, year, month, day)
            datePickerDialog.datePicker.minDate = System.currentTimeMillis()
            datePickerDialog.show()
        }

        chatUser.setOnClickListener {
            val direction = CustomerMapFragmentDirections.actionCustomerMapFragmentToChatFragment()
            it.findNavController().navigate(direction)
        }

        newRatingButton.setOnClickListener {
            val ratingRef = FirebaseHelper.getUserRating(driverFoundID!!)
            val ratingRefId = ratingRef.push().key

            val map = hashMapOf<String, Any?>("value" to newRatingBar.rating, "comment" to newRatingText.text.toString())
            ratingRef.child(ratingRefId!!).updateChildren(map)

            switchState()
        }

        payment.setOnClickListener { setStatusSynced(Status.Rating, true) }
        rideStatus.setOnClickListener { switchState() }

        getActiveRequest()

        return binding.root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        super.onMapReady(googleMap)

        mMap.setOnMarkerClickListener { marker ->
            if (destinationMap.contains(marker)) {
                marker.remove()
                destinationList.remove(destinationMap[marker])
                destinationMap.remove(marker)

                if (!destinationList.isEmpty())
                    mLastLocation?.let { createMarkerRoute(LatLng(it.latitude, it.longitude), getLatLngList()) }
            }
            true
        }

        mMap.setOnMyLocationButtonClickListener {
            followMeFlag = true
            mLastLocation?.let { moveCamera(it) }
            true
        }
    }

    override fun onLocationChanged(location: Location?) {
        super.onLocationChanged(location)
        handleDriversAround()
    }

    override fun getActiveRequest() {
        activeRequestRef = FirebaseHelper.getUserActiveRequest(currentUserId)
        activeRequestListener = activeRequestRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists())
                    getRequestInfo(dataSnapshot.value.toString())
                else
                    killRideRequest()
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

                    driverFoundID = currentRequest?.driverId
                    status = currentRequest?.status!!

                    showStatusUI()
                } else
                    killRideRequest()
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun getAssignedDriverMessage() {
        val currentUser = SaveSharedPreference.getUserInfo(applicationContext)
        SaveSharedPreference.setChatSender(applicationContext, currentUser!!.username)
        SaveSharedPreference.setChatReceiver(applicationContext, driverFoundUsername!!)

        newIncomeMessageRef = FirebaseHelper.getMessage().child(currentUser.username + "_to_" + driverFoundUsername).child("newMessagePushed")
        newIncomeMessageListener = newIncomeMessageRef?.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) chatUser.text = "Message (!)"
            }
        })
    }

    private fun getAssignedDriverInfo(withRatings: Boolean) {
        val mDriverDatabase = FirebaseHelper.getUserInfo(driverFoundID!!)
        mDriverDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                    val userInfo = dataSnapshot.getValue(UserInfo::class.java)
                    userInfo ?: return

                    userInfo.name.let { userName.text = it }
                    userInfo.username.let { driverFoundUsername = it }
                    userInfo.phone.let { userPhone.text = it }
                    userInfo.imageUrl?.let { ActivityHelper.bindImageFromUrl(userProfileImage, it) }

                    if (showMessages) getAssignedDriverMessage()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })

        val driverDefaultCarRef = FirebaseHelper.getUserDefaultCar(driverFoundID!!)
        driverDefaultCarRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val defaultCarId = dataSnapshot.value.toString()
                val mDriverCar = FirebaseHelper.getCarKey(defaultCarId)
                mDriverCar.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {}

                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                            val carInfo = dataSnapshot.getValue(Car::class.java)
                            carInfo ?: return

                            carInfo.brand.let { carBrand.text = it }
                            carInfo.model.let { carModel.text = it }
                            carInfo.plate.let { carPlate.text = it }
                            carInfo.imageUrl?.let { ActivityHelper.bindImageFromUrl(carImage, it) }
                        }
                    }

                })
            }

        })

        if (withRatings) {
            val mDriverRating = FirebaseHelper.getUserRating(driverFoundID!!)
            mDriverRating.addListenerForSingleValueEvent(object : ValueEventListener {
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
    }

    private fun getAssignedDriverLocation() {
        rideStatus.text = getString(R.string.looking_driver_loc)

        driverLocationRef = FirebaseHelper.getUserLocation(driverFoundID!!)
        driverLocationListener = driverLocationRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val map = dataSnapshot.value as List<Any?>

                    val locationLat: Double = map[0]?.toString()?.toDouble() ?: 0.0
                    val locationLng: Double = map[1]?.toString()?.toDouble() ?: 0.0

                    val latLng = LatLng(locationLat, locationLng)

                    val loc1 = Location("")
                    loc1.latitude = pickupLatLng!!.latitude
                    loc1.longitude = pickupLatLng!!.longitude

                    val loc2 = Location("")
                    loc2.latitude = latLng.latitude
                    loc2.longitude = latLng.longitude

                    val distance: Float = loc1.distanceTo(loc2)

                    rideStatus.text = if (distance < 100) getString(R.string.driver_here) else getString(R.string.distance).plus(distance.toString())

                    mDriverMarker?.remove()
                    mDriverMarker = mMap.addMarker(MarkerOptions().position(latLng).title(getString(R.string.your_driver)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car)))
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    override fun startRideRequest() {
        currentRequest = Request(customerId = currentUserId,
                pickupLocation = mLastLocation!!,
                locationList = destinationList,
                requestDate = dateOfRide!!)
        FirebaseHelper.createRequest(currentRequest!!)

        showPendingUI()
    }

    override fun killRideRequest() {
        requestListener?.let { requestRef?.removeEventListener(it) }
        if (successfulRide) return

        currentRequest?.let { FirebaseHelper.removeRequest(it) }
        showFreshUI()
    }

    override fun completeRideRequest() {
        requestListener?.let { requestRef?.removeEventListener(it) }
        FirebaseHelper.completeRequest(currentRequest!!)

        showFreshUI()
    }

    private fun handleDriversAround() {
        if (showDriversAround) {
            if (shouldRefreshDriversAround()) getDriversAround()
        } else {
            driversAroundQuery?.removeAllListeners()

            for (key in driversAroundMarkerMap.keys) {
                driversAroundMarkerMap[key]?.remove()
                driversAroundMarkerMap.remove(key)
            }
        }
    }

    private fun shouldRefreshDriversAround(): Boolean {
        mLastLocation ?: return false

        val distance: Float = driversAroundLatestLoc?.distanceTo(mLastLocation)
                ?: DEFAULT_SEARCH_LOC_DIF+1
        return distance > DEFAULT_SEARCH_LOC_DIF
    }

    private fun getDriversAround() {
        val driversLocation = FirebaseHelper.getDriversAvailable()
        val geoFire = GeoFire(driversLocation)

        driversAroundLatestLoc = mLastLocation
        driversAroundQuery = mLastLocation?.let { geoFire.queryAtLocation(GeoLocation(it.longitude, it.latitude), DEFAULT_SEARCH_RADIUS) }

        driversAroundQuery?.removeAllListeners()
        driversAroundQuery?.addGeoQueryEventListener(object : GeoQueryEventListener {
            override fun onKeyEntered(key: String, location: GeoLocation) {
                if (driversAroundMarkerMap.containsKey(key)) return

                val driverLocation = LatLng(location.latitude, location.longitude)
                val mDriverMarker = mMap.addMarker(MarkerOptions().position(driverLocation).title(key).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car)))
                mDriverMarker?.let { driversAroundMarkerMap[key] = it }
            }

            override fun onKeyExited(key: String) {
                driversAroundMarkerMap[key]?.remove()
                driversAroundMarkerMap.remove(key)
            }

            override fun onKeyMoved(key: String, location: GeoLocation) {
                driversAroundMarkerMap[key]?.position = LatLng(location.latitude, location.longitude)
            }

            override fun onGeoQueryReady() {}

            override fun onGeoQueryError(error: DatabaseError) {}
        })
    }

    private fun getLatLngList(): ArrayList<LatLng> {
        val latLngList = ArrayList<LatLng>()
        for (place in destinationList) latLngList.add(place.latLng)
        return latLngList
    }

    override fun clearDestinationInfo() {
        super.clearDestinationInfo()

        for (marker in destinationMap.keys) marker.remove()
        destinationMap.clear()
        destinationList.clear()
    }

    private fun clearDriversInfo() {
        currentRequest = null
        driverFoundID = null
        driverFoundUsername = null
        showMessages = false

        /*
        ----------------------------------
        UI
        ----------------------------------
        */
        searchRequest.visibility = View.VISIBLE

        popup.visibility = View.GONE

        destination.text = ""

        userAllInfo.visibility = View.GONE
        userInfo.visibility = View.GONE
        carInfo.visibility = View.GONE

        userProfileImage.setImageResource(R.mipmap.ic_default_user)
        userName.text = ""
        userPhone.text = ""

        carImage.setImageResource(R.mipmap.ic_car)
        carPlate.text = ""
        carModel.text = ""
        carBrand.text = ""

        currentRating.visibility = View.GONE
        currentRatingBar.rating = 0.toFloat()
        currentRatingAvg.text = ""

        newRating.visibility = View.GONE
        newRatingBar.rating = 0.toFloat()
        newRatingText.setText("")

        communicateUser.visibility = View.GONE
        paymentInfo.visibility = View.GONE

        rideStatus.visibility = View.GONE
        rideStatus.isClickable = true
        rideStatus.text = getString(R.string.call_uber)
        /*
        ----------------------------------
        UI
        ----------------------------------
        */
    }

    override fun showFreshUI() {
        status = Status.Free

        showDriversAround = true
        showMessages = false

        pickupMarker?.remove()
        mDriverMarker?.remove()

        cleanMarkerRoute()
        clearDestinationInfo()
        clearDriversInfo()

        requestListener?.let { requestRef?.removeEventListener(it) }
        driverLocationListener?.let { driverLocationRef?.removeEventListener(it) }
        newIncomeMessageListener?.let { newIncomeMessageRef?.removeEventListener(it) }
    }

    override fun showPendingUI() {
        status = Status.Pending

        /*
        ----------------------------------
        UI
        ----------------------------------
        */
        searchRequest.visibility = View.GONE

        popup.visibility = View.GONE

        destination.text = getDestinationAsString()

        userAllInfo.visibility = View.GONE
        userInfo.visibility = View.GONE
        carInfo.visibility = View.GONE

        userProfileImage.setImageResource(R.mipmap.ic_default_user)
        userName.text = ""
        userPhone.text = ""

        carImage.setImageResource(R.mipmap.ic_car)
        carPlate.text = ""
        carModel.text = ""
        carBrand.text = ""

        currentRating.visibility = View.GONE
        currentRatingBar.rating = 0.toFloat()
        currentRatingAvg.text = ""

        newRating.visibility = View.GONE
        newRatingBar.rating = 0.toFloat()
        newRatingText.setText("")

        communicateUser.visibility = View.GONE
        paymentInfo.visibility = View.GONE

        rideStatus.visibility = View.VISIBLE
        rideStatus.isClickable = true
        rideStatus.text = getString(R.string.getting_driver)
        /*
        ----------------------------------
        UI
        ----------------------------------
        */

        showDriversAround = true
        isPickedUp = false
        successfulRide = false

        pickupLatLng = LatLng(currentRequest!!.pickupLocation!!.lat, currentRequest!!.pickupLocation!!.lng)
        pickupMarker?.remove()
        pickupMarker = mMap.addMarker(MarkerOptions().position(pickupLatLng!!).title(getString(R.string.pickup_here)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)))

        cleanMarkerRoute()

        pickupLatLng?.let { createMarkerRoute(LatLng(it.latitude, it.longitude), getLatLngList()) }

        driverLocationListener?.let { driverLocationRef?.removeEventListener(it) }
        newIncomeMessageListener?.let { newIncomeMessageRef?.removeEventListener(it) }
    }

    override fun showDriverToCustomerUI() {
        status = Status.DriverToCustomer

        /*
        ----------------------------------
        UI
        ----------------------------------
        */
        searchRequest.visibility = View.GONE

        popup.visibility = View.VISIBLE

        destination.text = getDestinationAsString()

        userAllInfo.visibility = View.VISIBLE
        userInfo.visibility = View.VISIBLE
        carInfo.visibility = View.VISIBLE

        userProfileImage.setImageResource(R.mipmap.ic_default_user)
        userName.text = ""
        userPhone.text = ""

        carImage.setImageResource(R.mipmap.ic_car)
        carPlate.text = ""
        carModel.text = ""
        carBrand.text = ""

        currentRating.visibility = View.VISIBLE
        currentRatingBar.rating = 0.toFloat()
        currentRatingAvg.text = ""

        newRating.visibility = View.GONE
        newRatingBar.rating = 0.toFloat()
        newRatingText.setText("")

        communicateUser.visibility = View.VISIBLE
        paymentInfo.visibility = View.GONE

        rideStatus.visibility = View.VISIBLE
        rideStatus.isClickable = true
        rideStatus.text = getString(R.string.driver_coming)
        /*
        ----------------------------------
        UI
        ----------------------------------
        */

        showMessages = true

        pickupLatLng = LatLng(currentRequest!!.pickupLocation!!.lat, currentRequest!!.pickupLocation!!.lng)
        pickupMarker?.remove()
        pickupMarker = mMap.addMarker(MarkerOptions().position(pickupLatLng!!).title(getString(R.string.pickup_here)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)))

        cleanMarkerRoute()

        driverLocationListener?.let { driverLocationRef?.removeEventListener(it) }
        newIncomeMessageListener?.let { newIncomeMessageRef?.removeEventListener(it) }

        getAssignedDriverInfo(true)
        getAssignedDriverLocation()
    }

    override fun showToDestinationUI() {
        status = Status.ToDestination

        /*
        ----------------------------------
        UI
        ----------------------------------
        */
        searchRequest.visibility = View.GONE

        popup.visibility = View.GONE

        destination.text = getDestinationAsString()

        userAllInfo.visibility = View.GONE
        userInfo.visibility = View.GONE
        carInfo.visibility = View.GONE

        userProfileImage.setImageResource(R.mipmap.ic_default_user)
        userName.text = ""
        userPhone.text = ""

        carImage.setImageResource(R.mipmap.ic_car)
        carPlate.text = ""
        carModel.text = ""
        carBrand.text = ""

        currentRating.visibility = View.GONE
        currentRatingBar.rating = 0.toFloat()
        currentRatingAvg.text = ""

        newRating.visibility = View.GONE
        newRatingBar.rating = 0.toFloat()
        newRatingText.setText("")

        communicateUser.visibility = View.GONE
        paymentInfo.visibility = View.GONE

        rideStatus.visibility = View.VISIBLE
        rideStatus.isClickable = false
        rideStatus.text = getString(R.string.ride_started)
        /*
        ----------------------------------
        UI
        ----------------------------------
        */

        showDriversAround = false

        isPickedUp = true
        successfulRide = false
        showMessages = false

        pickupLatLng = LatLng(currentRequest!!.pickupLocation!!.lat, currentRequest!!.pickupLocation!!.lng)
        pickupMarker?.remove()
        pickupMarker = mMap.addMarker(MarkerOptions().position(pickupLatLng!!).title(getString(R.string.pickup_here)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)))

        cleanMarkerRoute()
        clearDestinationInfo()
        syncRequestDestination()

        mLastLocation?.let { createMarkerRoute(LatLng(it.latitude, it.longitude), destinationLatLngList) }

        driverLocationListener?.let { driverLocationRef?.removeEventListener(it) }
        newIncomeMessageListener?.let { newIncomeMessageRef?.removeEventListener(it) }
    }

    override fun showPaymentUI() {
        status = Status.Payment

        /*
        ----------------------------------
        UI
        ----------------------------------
        */
        searchRequest.visibility = View.GONE

        popup.visibility = View.GONE

        destination.text = getDestinationAsString()

        userAllInfo.visibility = View.GONE
        userInfo.visibility = View.GONE
        carInfo.visibility = View.GONE

        userProfileImage.setImageResource(R.mipmap.ic_default_user)
        userName.text = ""
        userPhone.text = ""

        carImage.setImageResource(R.mipmap.ic_car)
        carPlate.text = ""
        carModel.text = ""
        carBrand.text = ""

        currentRating.visibility = View.GONE
        currentRatingBar.rating = 0.toFloat()
        currentRatingAvg.text = ""

        newRating.visibility = View.GONE
        newRatingBar.rating = 0.toFloat()
        newRatingText.setText("")

        communicateUser.visibility = View.GONE
        paymentInfo.visibility = View.VISIBLE
        payment.text =
                (if (currentRequest!!.payByCard) getString(R.string.pay_with_card)
                else getString(R.string.pay_with_cash)) + ": " + currentRequest!!.amount + "€"

        rideStatus.visibility = View.GONE
        rideStatus.isClickable = true
        rideStatus.text = getString(R.string.cancel)
        /*
        ----------------------------------
        UI
        ----------------------------------
        */

        successfulRide = false
        isPickedUp = false
        showMessages = false

        cleanMarkerRoute()
        clearDestinationInfo()

        driverLocationListener?.let { driverLocationRef?.removeEventListener(it) }
        newIncomeMessageListener?.let { newIncomeMessageRef?.removeEventListener(it) }
    }


    override fun showRatingUI() {
        status = Status.Rating

        /*
        ----------------------------------
        UI
        ----------------------------------
        */
        searchRequest.visibility = View.GONE

        popup.visibility = View.VISIBLE

        destination.text = getDestinationAsString()

        userAllInfo.visibility = View.VISIBLE
        userInfo.visibility = View.VISIBLE
        carInfo.visibility = View.GONE

        userProfileImage.setImageResource(R.mipmap.ic_default_user)
        userName.text = ""
        userPhone.text = ""

        carImage.setImageResource(R.mipmap.ic_car)
        carPlate.text = ""
        carModel.text = ""
        carBrand.text = ""

        currentRating.visibility = View.GONE
        currentRatingBar.rating = 0.toFloat()
        currentRatingAvg.text = ""

        newRating.visibility = View.VISIBLE
        newRatingBar.rating = 0.toFloat()
        newRatingText.setText("")

        communicateUser.visibility = View.GONE
        paymentInfo.visibility = View.GONE

        rideStatus.visibility = View.GONE
        rideStatus.isClickable = true
        rideStatus.text = getString(R.string.cancel)
        /*
        ----------------------------------
        UI
        ----------------------------------
        */

        successfulRide = false
        isPickedUp = false
        showMessages = false

        cleanMarkerRoute()
        clearDestinationInfo()

        requestListener?.let { requestRef?.removeEventListener(it) }
        driverLocationListener?.let { driverLocationRef?.removeEventListener(it) }
        newIncomeMessageListener?.let { newIncomeMessageRef?.removeEventListener(it) }

        getAssignedDriverInfo(false)
    }

    override fun showStatusUI() {
        when (status) {
            Status.Free -> {
                requestListener?.let { requestRef?.removeEventListener(it) }
            }
            Status.Pending -> {
                showPendingUI()
            }
            Status.DriverToCustomer -> {
                showDriverToCustomerUI()
            }
            Status.ToDestination -> {
                showToDestinationUI()
            }
            Status.Payment -> {
                showPaymentUI()
            }
            Status.Rating -> {
                showRatingUI()
            }
            Status.RatingDone -> {
            }
            Status.Done -> {
                successfulRide = true
                requestListener?.let { requestRef?.removeEventListener(it) }

                showFreshUI()
            }
        }
    }

    override fun switchState() {
        when (status) {
            Status.Free -> {
                startRideRequest()
            }
            Status.Pending -> {
                killRideRequest()
            }
            Status.DriverToCustomer -> {
                currentRequest?.let { it.pickupTime = getCurrentTimestamp() }
                setStatusSynced(Status.ToDestination, true)
            }
            Status.ToDestination -> {
            }
            Status.Payment -> {
                setStatusSynced(Status.Rating, true)
            }
            Status.Rating -> {
                setStatusSynced(Status.RatingDone, true)
            }
            Status.RatingDone -> {
                setStatusSynced(Status.Done, true)
                showFreshUI()
            }
            Status.Done -> {
            }
        }
    }

}