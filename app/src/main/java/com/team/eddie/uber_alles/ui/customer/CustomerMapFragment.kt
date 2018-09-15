package com.team.eddie.uber_alles.ui.customer

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.navigation.findNavController
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.common.api.Status
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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.databinding.FragmentCustomerMapBinding
import com.team.eddie.uber_alles.ui.ActivityHelper
import com.team.eddie.uber_alles.ui.generic.GenericMapFragment
import com.team.eddie.uber_alles.utils.SaveSharedPreference
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper.CUSTOMER_RIDE_ID
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper.DESTINATION
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper.DESTINATION_LAT
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper.DESTINATION_LOT
import com.team.eddie.uber_alles.utils.firebase.Request
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList


class CustomerMapFragment : GenericMapFragment() {

    private lateinit var binding: FragmentCustomerMapBinding

    private var pickupLocationQuery: GeoQuery? = null
    private var pickupLocation: LatLng? = null
    private var pickupMarker: Marker? = null

    private var radius = 1
    private var driverFound: Boolean = false
    private var driverFoundID: String? = null

    private lateinit var autocompleteFragment: SupportPlaceAutocompleteFragment

    private lateinit var mDriverInfo: LinearLayout
    private lateinit var mDriverProfileImage: ImageView

    private lateinit var mRequest: MaterialButton

    private lateinit var mDriverName: TextView
    private lateinit var mDriverPhone: TextView
    private lateinit var mDriverCar: TextView

    private lateinit var mRatingBar: RatingBar

    private lateinit var ratingTextLayout: TextInputLayout
    private lateinit var mRatingText: TextInputEditText
    private lateinit var mRatingButton: MaterialButton
    private var mRatingAvg: TextView? = null

    private var destination: String? = null
    private var destinationLatLng: LatLng? = null
    private var destinationMarker: Marker? = null
    private var destinationList: ArrayList<Location> = ArrayList()

    private var completedRide: Boolean = false
    private var isPickedUp: Boolean = false

    private var mDriverMarker: Marker? = null
    private var driverLocationRef: DatabaseReference? = null
    private var driverLocationListener: ValueEventListener? = null

    private var driveHasEndedRef: DatabaseReference? = null
    private var driveHasEndedListener: ValueEventListener? = null

    private var customerPickedUpRef: DatabaseReference? = null
    private var customerPickedUpListener: ValueEventListener? = null

    private var newIncomeMessageRef: DatabaseReference? = null
    private var newIncomeMessageListener: ValueEventListener? = null

    // drivers around vars
    private val DEFAULT_SEARCH_RADIUS: Double = 5000.0
    private val DEFAULT_SEARCH_LOC_DIF: Float = 1F

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

        mDriverInfo = binding.driverInfo
        mDriverProfileImage = binding.driverProfileImage

        mRequest = binding.request

        mDriverName = binding.driverName
        mDriverPhone = binding.driverPhone
        mDriverCar = binding.driverCar

        mRatingBar = binding.ratingBar
        mRatingText = binding.ratingText
        ratingTextLayout = binding.ratingTextLayout
        mRatingButton = binding.ratingButton
        mRatingAvg = binding.ratingAvg


        autocompleteFragment = childFragmentManager.findFragmentById(R.id.place_autocomplete_fragment) as SupportPlaceAutocompleteFragment
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                destination = place.name.toString()
                destinationLatLng = place.latLng

                val newDestination = Location("")
                newDestination.latitude = destinationLatLng!!.latitude
                newDestination.longitude = destinationLatLng!!.longitude
                destinationList.add(newDestination)

                destinationMarker?.remove()
                destinationMarker = mMap.addMarker(MarkerOptions().position(place.latLng).title(getString(R.string.destination_here)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)))

                moveCamera(place.latLng)
                followMeFlag = false

                mRequest.visibility = View.VISIBLE
            }

            override fun onError(status: Status) {}
        })

        mRequest.setOnClickListener {
            //            if (SaveSharedPreference.getActiveRequest(applicationContext))
//                endRide()
//            else
            startRideRequest()
        }

        binding.chatDriver.setOnClickListener {
            val direction = CustomerMapFragmentDirections.actionCustomerMapFragmentToChatFragment()
            it.findNavController().navigate(direction)
        }

        mRatingButton.setOnClickListener {

            val ratingRef = FirebaseHelper.getUserRating(driverFoundID!!)
            val ratingRefId = ratingRef.push().key

            val map = hashMapOf<String, Any?>("value" to mRatingBar.rating/*,"comment" to mRatingText*/)

            ratingRef.child(ratingRefId!!).updateChildren(map)

            clearDriversInfo()
        }

        return binding.root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        super.onMapReady(googleMap)

        mMap.setOnMyLocationButtonClickListener {
            followMeFlag = true
            mLastLocation?.let { moveCamera(it) }
            true
        }
    }

    override fun onLocationChanged(location: Location?) {
        super.onLocationChanged(location)

        if (shouldRefreshDriversAround()) getDriversAround()
    }

    private fun getClosestDriver() {
        val driversAvailable = FirebaseHelper.getDriversAvailable()
        val geoFire = GeoFire(driversAvailable)

        pickupLocationQuery = pickupLocation?.let { geoFire.queryAtLocation(GeoLocation(it.latitude, it.longitude), radius.toDouble()) }

        pickupLocationQuery?.removeAllListeners()
        pickupLocationQuery?.addGeoQueryEventListener(object : GeoQueryEventListener {
            override fun onKeyEntered(key: String, location: GeoLocation) {
                if (!driverFound && SaveSharedPreference.getActiveRequest(applicationContext)) {
                    driverFound = true
                    driverFoundID = key

                    val driverCustReqRef = FirebaseHelper.getDriverCustomerReq(driverFoundID!!)

                    val map: HashMap<String, Any?> = hashMapOf(
                            CUSTOMER_RIDE_ID to currentUserId,
                            DESTINATION to destination,
                            DESTINATION_LAT to (destinationLatLng?.latitude),
                            DESTINATION_LOT to (destinationLatLng?.longitude))
                    driverCustReqRef.updateChildren(map)

                    getHasCustomerPickedUp()
                    getUserInfo()
                    getHasRideEnded()

                    mRequest.text = getString(R.string.looking_driver_loc)
                }
            }

            override fun onKeyExited(key: String) {

            }

            override fun onKeyMoved(key: String, location: GeoLocation) {

            }

            override fun onGeoQueryReady() {
                if (!driverFound) {
                    radius++
                    getClosestDriver()
                }
            }

            override fun onGeoQueryError(error: DatabaseError) {

            }
        })
    }

    private fun getDriverLocation() {
        driverLocationRef = FirebaseHelper.getDriversWorkingLocation(driverFoundID!!)
        driverLocationListener = driverLocationRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && SaveSharedPreference.getActiveRequest(applicationContext)) {
                    val map = dataSnapshot.value as List<Any?>

                    val locationLat: Double = map[0]?.toString()?.toDouble() ?: 0.0
                    val locationLng: Double = map[1]?.toString()?.toDouble() ?: 0.0

                    val driverLatLng = LatLng(locationLat, locationLng)

                    val loc1 = Location("")
                    loc1.latitude = pickupLocation!!.latitude
                    loc1.longitude = pickupLocation!!.longitude

                    val loc2 = Location("")
                    loc2.latitude = driverLatLng.latitude
                    loc2.longitude = driverLatLng.longitude

                    val distance: Float = loc1.distanceTo(loc2)

                    mRequest.text = if (distance < 100) getString(R.string.driver_here) else getString(R.string.driver_found).plus(distance.toString())

                    mDriverMarker?.remove()
                    mDriverMarker = mMap.addMarker(MarkerOptions().position(driverLatLng).title(getString(R.string.your_driver)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car)))
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })

    }

    private fun getUserInfo() {
        mDriverInfo.visibility = View.VISIBLE

        val mDriverDatabase = FirebaseHelper.getUserInfo(driverFoundID!!)
        mDriverDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                    val map = dataSnapshot.value as Map<String, Any>

                    map["name"]?.let { mDriverName.text = it.toString() }
                    map["phone"]?.let { mDriverPhone.text = it.toString() }
                    map["car"]?.let { mDriverCar.text = it.toString() }
                    map["profileImageUrl"]?.let { ActivityHelper.bindImageFromUrl(mDriverProfileImage, it) }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })

        val mCustomerRating = FirebaseHelper.getUserRating(driverFoundID!!)
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
                mRatingAvg?.text = "Average Rating: " + df.format(ratingsAvg).toString() + "/5"
            }

        })
    }

    private fun getHasCustomerPickedUp() {
        customerPickedUpRef = FirebaseHelper.getDriverCustomerReqPickup(driverFoundID!!)
        customerPickedUpListener = customerPickedUpRef!!.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.exists() && !isPickedUp) {
                    getDriverLocation()

                    SaveSharedPreference.setChatSender(applicationContext, currentUserId)
                    SaveSharedPreference.setChatReceiver(applicationContext, driverFoundID!!)
                    newIncomeMessageRef = FirebaseHelper.getMessage().child(currentUserId + "_to_" + driverFoundID).child("newMessagePushed")

                    binding.callDriver.visibility = View.VISIBLE
                    binding.chatDriver.visibility = View.VISIBLE

                    newIncomeMessageListener = newIncomeMessageRef?.addValueEventListener(object : ValueEventListener {
                        override fun onCancelled(p0: DatabaseError) {}

                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) binding.chatDriver.text = "Message (!)"
                        }

                    })
                } else {

                    isPickedUp = true
                    binding.callDriver.visibility = View.GONE
                    binding.chatDriver.visibility = View.GONE

                    mRequest.text = getString(R.string.ride_started)
                    mRequest.isClickable = false
                    getRouteToMarker(destinationLatLng)

                    driverLocationRef?.removeEventListener(driverLocationListener!!)
                }
            }

            override fun onCancelled(p0: DatabaseError) {}

        })
    }

    private fun getHasRideEnded() {
        driveHasEndedRef = FirebaseHelper.getDriverCustomerRide(driverFoundID!!)
        driveHasEndedListener = driveHasEndedRef!!.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.exists()) {
                    completedRide = true
                    endRide()
                }
            }

            override fun onCancelled(p0: DatabaseError) {}

        })
    }

    private fun startRideRequest() {
        val request = Request(customerId = currentUserId, pickupLocation = mLastLocation!!, locationList = destinationList)
        FirebaseHelper.createRequest(request)

        SaveSharedPreference.setActiveRequest(applicationContext, true)

        val ref = FirebaseHelper.getCustomerRequest()
        GeoFire(ref).setLocation(currentUserId, GeoLocation(mLastLocation!!.latitude, mLastLocation!!.longitude))

        pickupLocation = LatLng(mLastLocation!!.latitude, mLastLocation!!.longitude)
        pickupMarker = mMap.addMarker(MarkerOptions().position(pickupLocation!!).title(getString(R.string.pickup_here)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)))

        autocompleteFragment.view?.visibility = View.GONE
        mRequest.text = getString(R.string.getting_driver)

        getClosestDriver()
    }

    private fun endRide() {
        SaveSharedPreference.setActiveRequest(applicationContext, false)
        radius = 1

        pickupLocationQuery!!.removeAllListeners()
        driverLocationRef?.removeEventListener(driverLocationListener!!)
        driveHasEndedRef?.removeEventListener(driveHasEndedListener!!)
        newIncomeMessageRef?.removeEventListener(newIncomeMessageListener!!)
        customerPickedUpRef?.removeEventListener(customerPickedUpListener!!)

        val ref = FirebaseHelper.getCustomerRequest()
        val geoFire = GeoFire(ref)

        geoFire.removeLocation(currentUserId)

        pickupMarker?.remove()
        destinationMarker?.remove()
        mDriverMarker?.remove()

        erasePolylines()

        autocompleteFragment.view?.visibility = View.VISIBLE

        if (completedRide) {
            completedRide = false
            isPickedUp = false
            mRatingBar.rating = 0.toFloat()
            mRatingBar.setIsIndicator(false)
            mRatingBar.numStars = 5
            mRatingAvg?.visibility = View.GONE
            mRatingButton.visibility = View.VISIBLE
            ratingTextLayout.visibility = View.VISIBLE
            mRequest.text = getString(R.string.ride_ended)
        } else
            clearDriversInfo()
    }

    private fun clearDriversInfo() {
        driverFoundID?.let {
            val driverRef = FirebaseHelper.getDriverCustomerReq(it)
            driverRef.removeValue()
            driverFoundID = null
            driverFound = false
        }

        mDriverInfo.visibility = View.GONE
        mDriverName.text = ""
        mDriverPhone.text = ""
        mDriverCar.text = ""
        mDriverProfileImage.setImageResource(R.mipmap.ic_default_user)

        mRatingButton.visibility = View.GONE
        ratingTextLayout.visibility = View.GONE
        mRatingBar.rating = 0.toFloat()
        mRatingBar.setIsIndicator(false)
        mRatingBar.numStars = 1
        mRatingAvg?.text = ""
        mRatingAvg?.visibility = View.VISIBLE
        mRatingText.setText("", TextView.BufferType.EDITABLE)

        mRequest.isClickable = true
        mRequest.visibility = View.GONE
        mRequest.text = getString(R.string.call_uber)
    }

    private fun shouldRefreshDriversAround(): Boolean {
        mLastLocation ?: return false

        val distance: Float = driversAroundLatestLoc?.distanceTo(mLastLocation)
                ?: DEFAULT_SEARCH_LOC_DIF+1
        return showDriversAround && distance > DEFAULT_SEARCH_LOC_DIF
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
}
