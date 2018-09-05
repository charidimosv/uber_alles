package com.team.eddie.uber_alles.ui.customer

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bumptech.glide.Glide
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
import com.google.firebase.database.*
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.databinding.FragmentCustomerMapBinding
import com.team.eddie.uber_alles.ui.generic.GenericMapFragment
import com.team.eddie.uber_alles.utils.FirebaseHelper
import com.team.eddie.uber_alles.utils.FirebaseHelper.CUSTOMER_RIDE_ID
import com.team.eddie.uber_alles.utils.FirebaseHelper.DESTINATION
import com.team.eddie.uber_alles.utils.FirebaseHelper.DESTINATION_LAT
import com.team.eddie.uber_alles.utils.FirebaseHelper.DESTINATION_LOT
import com.team.eddie.uber_alles.utils.SaveSharedPreference


class CustomerMapFragment : GenericMapFragment() {

    lateinit var binding: FragmentCustomerMapBinding

    private var geoQuery: GeoQuery? = null
    private var pickupLocation: LatLng? = null
    private var pickupMarker: Marker? = null

    private var radius = 1
    private var driverFound: Boolean = false
    private var driverFoundID: String? = null

    private var mDriverMarker: Marker? = null
    private var driverLocationRef: DatabaseReference? = null
    private var driverLocationRefListener: ValueEventListener? = null

    private var driveHasEndedRef: DatabaseReference? = null
    private var driveHasEndedRefListener: ValueEventListener? = null

    var customerPickedUpRef: DatabaseReference? = null
    var customerPickedUpRefListener: ValueEventListener? = null

    private var newIncomeMessageRef: DatabaseReference? = null
    private var newIncomeMessageListener: ValueEventListener? = null

    private lateinit var autocompleteFragment: SupportPlaceAutocompleteFragment

    private lateinit var mDriverInfo: LinearLayout
    private lateinit var mDriverProfileImage: ImageView

    private lateinit var mRequest: Button

    private lateinit var mDriverName: TextView
    private lateinit var mDriverPhone: TextView
    private lateinit var mDriverCar: TextView

    private lateinit var mRatingBar: RatingBar
    private lateinit var mRatingText: EditText
    private lateinit var mRatingButton: Button
    private var mRatingAvg: TextView? = null


    private var destination: String? = null
    private var destinationLatLng: LatLng? = null
    private var destinationMarker: Marker? = null

    private var completedRide: Boolean = false
    private var followMeFlag: Boolean = true
    private var isPickedUp: Boolean = false


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCustomerMapBinding.inflate(inflater, container, false)

        mDriverInfo = binding.driverInfo
        mDriverProfileImage = binding.driverProfileImage

        mRequest = binding.request

        mDriverName = binding.driverName
        mDriverPhone = binding.driverPhone
        mDriverCar = binding.driverCar

        mRatingBar = binding.ratingBar
        mRatingText = binding.ratingText
        mRatingButton = binding.ratingButton
        mRatingAvg =  binding.ratingAvg

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

        autocompleteFragment = childFragmentManager.findFragmentById(R.id.place_autocomplete_fragment) as SupportPlaceAutocompleteFragment
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                destination = place.name.toString()
                destinationLatLng = place.latLng

                destinationMarker?.remove()
                destinationMarker = mMap.addMarker(MarkerOptions().position(place.latLng).title(getString(R.string.destination_here)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)))

                moveCamera(place.latLng)
                followMeFlag = false

                mRequest.visibility = View.VISIBLE
            }

            override fun onError(status: Status) {}
        })

        mRequest.setOnClickListener {
            if (SaveSharedPreference.getActiveRequest(activity!!.applicationContext))
                endRide()
            else
                startRideRequest()
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
            if (mLastLocation != null) moveCamera(mLastLocation!!)
            true;
        };
    }

    override fun onLocationChanged(location: Location?) {
        if (activity!!.applicationContext != null && location != null) {
            mLastLocation = location
            if (followMeFlag) moveCamera(location)
        }
    }

    private fun getClosestDriver() {
        val driversAvailable: DatabaseReference = FirebaseHelper.getDriversAvailable()

        val geoFire = GeoFire(driversAvailable)
        geoQuery = geoFire.queryAtLocation(GeoLocation(pickupLocation!!.latitude, pickupLocation!!.longitude), radius.toDouble())
        geoQuery?.removeAllListeners()

        geoQuery?.addGeoQueryEventListener(object : GeoQueryEventListener {
            override fun onKeyEntered(key: String, location: GeoLocation) {
                if (!driverFound && SaveSharedPreference.getActiveRequest(activity!!.applicationContext)) {
                    driverFound = true
                    driverFoundID = key

                    val driverCustReqRef = FirebaseHelper.getDriverCustomerReq(driverFoundID!!)

                    val map: HashMap<String, Any?> = hashMapOf(
                            CUSTOMER_RIDE_ID to currentUserId,
                            DESTINATION to destination,
                            DESTINATION_LAT to (destinationLatLng?.latitude),
                            DESTINATION_LOT to (destinationLatLng?.longitude)
                    )
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
        driverLocationRefListener = driverLocationRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && SaveSharedPreference.getActiveRequest(activity!!.applicationContext)) {
                    val map: List<*> = dataSnapshot.value as List<*>

                    val locationLat: Double = if (map[0] == null) 0.0 else map[0].toString().toDouble()
                    val locationLng: Double = if (map[1] == null) 0.0 else map[1].toString().toDouble()

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

                    if (map["name"] != null)
                        mDriverName.text = map["name"].toString()

                    if (map["phone"] != null)
                        mDriverPhone.text = map["phone"].toString()

                    if (map["car"] != null)
                        mDriverCar.text = map["car"].toString()

                    if (map["profileImageUrl"] != null)
                        Glide.with(activity?.application!!).load(map["profileImageUrl"].toString()).into(mDriverProfileImage)

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
                for (rating in dataSnapshot.children) {
                    ratingSum += rating.child("value").value.toString().toFloat()
                    ratingsTotal++
                }
                if (ratingsTotal != 0.toFloat()) {
                    ratingsAvg = ratingSum / ratingsTotal
                    mRatingBar?.rating = ratingsAvg
                }
                mRatingAvg?.text = "Average Rating: "+ratingsAvg.toString()+"/5"
            }

        })


    }

    private fun getHasCustomerPickedUp() {
        customerPickedUpRef = FirebaseHelper.getDriverCustomerReqPickup(driverFoundID!!)
        customerPickedUpRefListener = customerPickedUpRef!!.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.exists() && !isPickedUp) {
                    getDriverLocation()

                    SaveSharedPreference.setChatSender(activity!!.applicationContext,currentUserId)
                    SaveSharedPreference.setChatReceiver(activity!!.applicationContext,driverFoundID!!)
                    newIncomeMessageRef = FirebaseHelper.getMessage().child(currentUserId+"_to_"+driverFoundID).child("newMessagePushed")

                    binding.callDriver.visibility =  View.VISIBLE
                    binding.chatDriver.visibility =  View.VISIBLE

                    newIncomeMessageListener = newIncomeMessageRef?.addValueEventListener(object :ValueEventListener{
                        override fun onCancelled(p0: DatabaseError) {}

                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if(dataSnapshot.exists()){
                                binding.chatDriver.text =  "Message (!)"
                            }
                        }

                    })
                }
                else {

                    isPickedUp=true
                    binding.callDriver.visibility =  View.GONE
                    binding.chatDriver.visibility =  View.GONE

                    mRequest.text = getString(R.string.ride_started)
                    mRequest.isClickable = false
                    getRouteToMarker(destinationLatLng)

                    driverLocationRef?.removeEventListener(driverLocationRefListener!!)
                }
            }

            override fun onCancelled(p0: DatabaseError) {}

        })
    }

    private fun getHasRideEnded() {
        driveHasEndedRef = FirebaseHelper.getDriverCustomerRide(driverFoundID!!)
        driveHasEndedRefListener = driveHasEndedRef!!.addValueEventListener(object : ValueEventListener {

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
        SaveSharedPreference.setActiveRequest(activity!!.applicationContext, true)

        val ref = FirebaseHelper.getCustomerRequest()
        GeoFire(ref).setLocation(currentUserId, GeoLocation(mLastLocation!!.latitude, mLastLocation!!.longitude))

        pickupLocation = LatLng(mLastLocation!!.latitude, mLastLocation!!.longitude)
        pickupMarker = mMap.addMarker(MarkerOptions().position(pickupLocation!!).title(getString(R.string.pickup_here)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)))

        autocompleteFragment.view?.visibility = View.GONE
        mRequest.text = getString(R.string.getting_driver)

        getClosestDriver()
    }

    private fun endRide() {
        SaveSharedPreference.setActiveRequest(activity!!.applicationContext, false)
        radius = 1

        geoQuery!!.removeAllListeners()
        driverLocationRef?.removeEventListener(driverLocationRefListener!!)
        driveHasEndedRef?.removeEventListener(driveHasEndedRefListener!!)
        newIncomeMessageRef?.removeEventListener(newIncomeMessageListener!!)
        customerPickedUpRef?.removeEventListener(customerPickedUpRefListener!!)

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
            mRatingBar?.setIsIndicator(false)
            mRatingBar?.numStars = 5
            mRatingAvg?.visibility = View.GONE
            mRatingButton.visibility = View.VISIBLE
            mRatingText.visibility = View.VISIBLE
            mRequest.text = getString(R.string.ride_ended)
        } else
            clearDriversInfo()
    }

    private fun clearDriversInfo() {
        if (driverFoundID != null) {
            val driverRef = FirebaseHelper.getDriverCustomerReq(driverFoundID!!)
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
        mRatingText.visibility = View.GONE
        mRatingBar.rating = 0.toFloat()
        mRatingBar?.setIsIndicator(false)
        mRatingBar?.numStars = 1
        mRatingAvg?.text = ""
        mRatingAvg?.visibility = View.VISIBLE
        mRatingText.setText("", TextView.BufferType.EDITABLE)

        mRequest.isClickable = true
        mRequest.visibility = View.GONE
        mRequest.text = getString(R.string.call_uber)
    }

}
