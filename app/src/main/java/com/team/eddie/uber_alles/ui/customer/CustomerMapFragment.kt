package com.team.eddie.uber_alles.ui.customer

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
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
import com.team.eddie.uber_alles.ui.session.WelcomeActivity
import com.team.eddie.uber_alles.utils.SaveSharedPreference
import java.util.*


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
    private var requestActive: Boolean = false

    private var isLoggingOut: Boolean = false

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

        binding.logoutButton.setOnClickListener {
            if (!requestActive) {
                isLoggingOut = true
                disconnectCustomer()
                SaveSharedPreference.cleanAll(activity!!.applicationContext)
                FirebaseAuth.getInstance().signOut()
                startActivity(WelcomeActivity.getLaunchIntent(activity!!))
            } else
                Toast.makeText(activity!!, "Ride must be ended before you can logout", Toast.LENGTH_SHORT).show()
        }

        binding.request.setOnClickListener {

            if (requestActive) {
                requestActive = false
                radius = 1
                geoQuery!!.removeAllListeners()
                driverLocationRef?.removeEventListener(driverLocationRefListener!!)

                if (driverFoundID != null) {
                    val driverRef = FirebaseDatabase.getInstance().reference.child("Users").child("Drivers").child(driverFoundID!!).child("customerRequest")
                    driverRef.setValue(true)
                    driverFoundID = null
                    driverFound = false
                }
                val userId = FirebaseAuth.getInstance().currentUser!!.uid
                val ref = FirebaseDatabase.getInstance().getReference("customerRequest")
                val geoFire = GeoFire(ref)
                geoFire.removeLocation(userId)
                pickupMarker?.remove()

                binding.request.setText("Call Uber")
            } else {
                requestActive = true
                val userId = FirebaseAuth.getInstance().currentUser!!.uid
                val ref = FirebaseDatabase.getInstance().getReference("customerRequest")
                val geoFire = GeoFire(ref)
                geoFire.setLocation(userId, GeoLocation(mLastLocation!!.latitude, mLastLocation!!.longitude))

                pickupLocation = LatLng(mLastLocation!!.latitude, mLastLocation!!.longitude)
                pickupMarker = mMap.addMarker(MarkerOptions().position(pickupLocation!!).title("Pickup Here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)))

                binding.request.setText("Getting your Driver....")

                getClosestDriver()
            }
        }

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

    private fun disconnectCustomer() {
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
    }

    private fun getClosestDriver() {
        val driverLocation: DatabaseReference = FirebaseDatabase.getInstance().reference.child("driversAvailable")

        val geoFire = GeoFire(driverLocation)
        geoQuery = geoFire.queryAtLocation(GeoLocation(pickupLocation!!.latitude, pickupLocation!!.longitude), radius.toDouble())
        geoQuery?.removeAllListeners()

        geoQuery?.addGeoQueryEventListener(object : GeoQueryEventListener {
            override fun onKeyEntered(key: String, location: GeoLocation) {
                if (!driverFound && requestActive) {
                    driverFound = true
                    driverFoundID = key

                    val driverRef = FirebaseDatabase.getInstance().reference.child("Users").child("Drivers").child(driverFoundID!!).child("customerRequest")
                    val customerId = FirebaseAuth.getInstance().currentUser!!.uid

                    val map: HashMap<String, String> = hashMapOf(
                            "customerRideId" to customerId
                    )
                    driverRef.updateChildren(map as Map<String, String>)

                    getDriverLocation()
                    binding.request.setText("Looking for Driver Location....")
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
                if (dataSnapshot.exists() && requestActive) {
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
}
