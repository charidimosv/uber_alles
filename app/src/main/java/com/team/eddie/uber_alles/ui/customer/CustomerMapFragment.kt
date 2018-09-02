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
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment
import com.google.android.gms.location.places.ui.PlaceSelectionListener
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.databinding.FragmentCustomerMapBinding
import com.team.eddie.uber_alles.ui.GenericMapFragment
import com.team.eddie.uber_alles.utils.SaveSharedPreference
import java.util.*
import kotlin.collections.HashMap


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

    //private var isLoggingOut: Boolean = false

    private var mDriverInfo: LinearLayout? = null

    private var mDriverProfileImage: ImageView? = null

    private var mDriverName: TextView? = null
    private var mDriverPhone: TextView? = null
    private var mDriverCar: TextView? = null
    private var mRatingBar: RatingBar? = null
    private var mRatingText: EditText? = null
    private var mRatingButton: Button? = null

    private var destination: String? = null
    private var destinationLatLng: LatLng? = null
    private var destinationMarker: Marker? = null

    private var completedRide: Boolean = false


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCustomerMapBinding.inflate(inflater, container, false)

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

        mDriverName = binding.driverName
        mDriverPhone = binding.driverPhone
        mDriverCar = binding.driverCar
        mRatingBar = binding.ratingBar
        mRatingText = binding.ratingText
        mRatingButton = binding.ratingButton


        binding.request.setOnClickListener {

            if (SaveSharedPreference.getActiveRequest(activity!!.applicationContext))
                endRide()
            else
                beginRide()
        }

        mRatingButton!!.setOnClickListener {

            val ratingRef = FirebaseDatabase.getInstance().reference.child("Users").child("Drivers").child(driverFoundID!!).child("rating")
            val ratingRefId = ratingRef.push().key

            val map = hashMapOf<String, Any?>("value" to mRatingBar!!.rating/*,"comment" to mRatingText*/)

            ratingRef.child(ratingRefId!!).updateChildren(map)

            clearDriversInfo()
        }

       /*val autocompleteFragment = fragmentManager?.findFragmentById(R.id.place_autocomplete_fragment) as PlaceAutocompleteFragment

        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                destination = place.name.toString()
                destinationLatLng = place.latLng
            }

            override fun onError(status: Status) {}
        }) */

        destination = "Louisville"
        destinationLatLng = LatLng(38.328732, -85.764771)


        return binding.root
    }

    override fun onLocationChanged(location: Location?) {
        if (activity!!.applicationContext != null && location != null) {
            mLastLocation = location
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), DEFAULT_ZOOM))

            /*val userId = FirebaseAuth.getInstance().currentUser!!.uid
            val refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable")
            val refWorking = FirebaseDatabase.getInstance().getReference("driversWorking")
            val geoFireAvailable = GeoFire(refAvailable)
            val geoFireWorking = GeoFire(refWorking)

            geoFireAvailable.setLocation(userId, GeoLocation(location.latitude, location.longitude))*/
        }
    }

   /* private fun disconnectCustomer() {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val custRequest = FirebaseDatabase.getInstance().getReference("customerRequest")
        val geoFire = GeoFire(custRequest)
        geoFire.removeLocation(userId)
        //val refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable")
        //val refWorking = FirebaseDatabase.getInstance().getReference("driversWorking")
        //val geoFireAvailable = GeoFire(refAvailable)
        //val geoFireWorking = GeoFire(refWorking)

        //geoFireAvailable.removeLocation(userId)
    }

    override fun onStop() {
        super.onStop()
        if (!isLoggingOut)
            disconnectCustomer()
    }*/

    private fun getClosestDriver() {
        val driverLocation: DatabaseReference = FirebaseDatabase.getInstance().reference.child("driversAvailable")

        val geoFire = GeoFire(driverLocation)
        geoQuery = geoFire.queryAtLocation(GeoLocation(pickupLocation!!.latitude, pickupLocation!!.longitude), radius.toDouble())
        geoQuery?.removeAllListeners()

        geoQuery?.addGeoQueryEventListener(object : GeoQueryEventListener {
            override fun onKeyEntered(key: String, location: GeoLocation) {
                if (!driverFound && SaveSharedPreference.getActiveRequest(activity!!.applicationContext)) {
                    driverFound = true
                    driverFoundID = key

                    val driverRef = FirebaseDatabase.getInstance().reference.child("Users").child("Drivers").child(driverFoundID!!).child("customerRequest")
                    val customerId = FirebaseAuth.getInstance().currentUser!!.uid

                    val map: HashMap<String, Any?> = hashMapOf(
                            "customerRideId" to customerId,
                            "destination" to destination,
                            "destinationLat" to (destinationLatLng?.latitude),
                            "destinationLng" to (destinationLatLng?.longitude)
                    )
                    driverRef.updateChildren(map as Map<String, String>)

                    getHasCustomerPickedUp()
                    getDriverInfo()
                    getHasRideEnded()
                    binding.request.text = "Looking for Driver Location...."
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
        driverLocationRef = FirebaseDatabase.getInstance().reference.child("driversWorking").child(driverFoundID!!).child("l")
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

                    binding.request.text = if (distance < 100) "Driver's Here" else "Driver Found: ".plus(distance.toString())

                    mDriverMarker?.remove()
                    mDriverMarker = mMap.addMarker(MarkerOptions().position(driverLatLng).title("your driver").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car)))
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })

    }

    private fun getDriverInfo() {
        mDriverInfo?.visibility = View.VISIBLE
        val mDriverDatabase = FirebaseDatabase.getInstance().reference.child("Users").child("Drivers").child(driverFoundID!!)
        mDriverDatabase?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                    val map = dataSnapshot.value as Map<String, Any>?
                    if (map!!["name"] != null)
                        mDriverName?.text = map["name"].toString()

                    if (map["phone"] != null)
                        mDriverPhone?.text = map["phone"].toString()

                    if (map["car"] != null)
                        mDriverCar?.text = map["car"].toString()

                    if (map["profileImageUrl"] != null)
                        Glide.with(activity?.application!!).load(map["profileImageUrl"].toString()).into(mDriverProfileImage!!)

                    //Load rating
                    var ratingSum = 0.toFloat()
                    var ratingsTotal = 0.toFloat()
                    var ratingsAvg = 0.toFloat()
                    for (rating in dataSnapshot.child("rating").children){
                        ratingSum += rating.child("value").value.toString().toFloat()
                        ratingsTotal++
                    }
                    if(ratingsTotal != 0.toFloat()){
                        ratingsAvg = ratingSum/ratingsTotal
                        mRatingBar?.rating = ratingsAvg
                    }

                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })

    }

    private fun getHasCustomerPickedUp() {
        customerPickedUpRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID!!).child("customerRequest").child("pickup");
        customerPickedUpRefListener = customerPickedUpRef!!.addValueEventListener(object : ValueEventListener{

            override fun onDataChange(dataSnapshot: DataSnapshot){
                if (!dataSnapshot.exists())
                    getDriverLocation()
                else {
                    binding.request.text = "Your ride has start"
                    binding.request.isClickable = false
                    getRouteToMarker(destinationLatLng)
                }
            }

            override fun onCancelled(p0: DatabaseError) {}

        })
    }

    private fun getHasRideEnded() {
        driveHasEndedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID!!).child("customerRequest").child("customerRideId");
        driveHasEndedRefListener = driveHasEndedRef!!.addValueEventListener(object : ValueEventListener{

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.exists()) {
                    completedRide = true
                    endRide()
                }
            }

            override fun onCancelled(p0: DatabaseError) {}

        })
    }

    private fun endRide(){
        SaveSharedPreference.setActiveRequest(activity!!.applicationContext, false)
        radius = 1
        geoQuery!!.removeAllListeners()
        driverLocationRef?.removeEventListener(driverLocationRefListener!!)
        driveHasEndedRef?.removeEventListener(driveHasEndedRefListener!!)
        customerPickedUpRef?.removeEventListener(customerPickedUpRefListener!!)


        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val ref = FirebaseDatabase.getInstance().getReference("customerRequest")
        val geoFire = GeoFire(ref)
        geoFire.removeLocation(userId)
        pickupMarker?.remove()
        destinationMarker?.remove()
        mDriverMarker?.remove()

        if(completedRide){
            completedRide = false
            mRatingBar?.rating = 0.toFloat()
            mRatingButton?.visibility = View.VISIBLE
            mRatingText?.visibility = View.VISIBLE
        }
        else
            clearDriversInfo()
    }

    private fun clearDriversInfo(){
        if (driverFoundID != null) {
            val driverRef = FirebaseDatabase.getInstance().reference.child("Users").child("Drivers").child(driverFoundID!!).child("customerRequest")
            driverRef.removeValue()
            driverFoundID = null
            driverFound = false
        }

        mDriverInfo?.visibility = View.GONE
        mDriverName?.text = ""
        mDriverPhone?.text = ""
        mDriverCar?.text = ""
        mDriverProfileImage?.setImageResource(R.mipmap.ic_default_user)

        mRatingButton?.visibility = View.GONE
        mRatingText?.visibility = View.GONE
        mRatingBar?.rating = 0.toFloat()
        mRatingText = null

        binding.request.isClickable = true
        binding.request.text = "Call Uber"
    }

    private fun beginRide() {
        SaveSharedPreference.setActiveRequest(activity!!.applicationContext, true)
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val ref = FirebaseDatabase.getInstance().getReference("customerRequest")
        val geoFire = GeoFire(ref)
        geoFire.setLocation(userId, GeoLocation(mLastLocation!!.latitude, mLastLocation!!.longitude))

        pickupLocation = LatLng(mLastLocation!!.latitude, mLastLocation!!.longitude)
        pickupMarker = mMap.addMarker(MarkerOptions().position(pickupLocation!!).title("Pickup Here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)))

        destinationMarker = mMap.addMarker(MarkerOptions().position(destinationLatLng!!).title("Leave Here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)))

        binding.request.text = "Getting your Driver...."

        getClosestDriver()
    }
}
