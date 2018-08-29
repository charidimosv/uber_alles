package com.team.eddie.uber_alles.ui.map

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.ui.welcome.WelcomeActivity
import com.team.eddie.uber_alles.utils.SaveSharedPreference
import kotlinx.android.synthetic.main.activity_map_customer.*
import java.util.*


class CustomerMapActivity : GenericMapActivity() {

    private var geoQuery: GeoQuery? = null
    private var pickupLocation: LatLng? = null
    private var pickupMarker: Marker? = null

    private var radius = 1
    private var driverFound: Boolean = false
    private var driverFoundID: String? = null

    private var mDriverMarker: Marker? = null
    private var driverLocationRef: DatabaseReference? = null
    private var driverLocationRefListener: ValueEventListener? = null

    companion object {
        fun getLaunchIntent(from: Context) = Intent(from, CustomerMapActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_map_customer)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.customer_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        logoutButton.setOnClickListener {
            SaveSharedPreference.cleanAll(applicationContext)
            FirebaseAuth.getInstance().signOut()
            startActivity(WelcomeActivity.getLaunchIntent(this))
        }

        request.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser!!.uid
            val ref = FirebaseDatabase.getInstance().getReference("customerRequest")
            val geoFire = GeoFire(ref)
            geoFire.setLocation(userId, GeoLocation(mLastLocation!!.latitude, mLastLocation!!.longitude))

            pickupLocation = LatLng(mLastLocation!!.latitude, mLastLocation!!.longitude)
            pickupMarker = mMap.addMarker(MarkerOptions().position(pickupLocation!!).title("Pickup Here").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)))

            request.setText("Getting your Driver....")

            getClosestDriver()
        }
    }

    override fun onLocationChanged(location: Location?) {
        if (applicationContext != null && location != null) {
            mLastLocation = location
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), DEFAULT_ZOOM))

            val userId = FirebaseAuth.getInstance().currentUser!!.uid
            val refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable")
            val refWorking = FirebaseDatabase.getInstance().getReference("driversWorking")
            val geoFireAvailable = GeoFire(refAvailable)
            val geoFireWorking = GeoFire(refWorking)

            geoFireAvailable.setLocation(userId, GeoLocation(location.latitude, location.longitude))
        }
    }

    override fun onStop() {
        super.onStop()

        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable")
        val refWorking = FirebaseDatabase.getInstance().getReference("driversWorking")
        val geoFireAvailable = GeoFire(refAvailable)
        val geoFireWorking = GeoFire(refWorking)

        geoFireAvailable.removeLocation(userId)
    }

    private fun getClosestDriver() {
        val driverLocation: DatabaseReference = FirebaseDatabase.getInstance().reference.child("driversAvailable")

        val geoFire = GeoFire(driverLocation)
        geoQuery = geoFire.queryAtLocation(GeoLocation(pickupLocation!!.latitude, pickupLocation!!.longitude), radius.toDouble())
        geoQuery?.removeAllListeners()

        geoQuery?.addGeoQueryEventListener(object : GeoQueryEventListener {
            override fun onKeyEntered(key: String, location: GeoLocation) {
                if (!driverFound) {
                    driverFound = true
                    driverFoundID = key

                    val driverRef = FirebaseDatabase.getInstance().reference.child("Users").child("Drivers").child(driverFoundID!!).child("customerRequest")
                    val customerId = FirebaseAuth.getInstance().currentUser!!.uid

                    val map: HashMap<String, String> = hashMapOf(
                            "customerRideId" to customerId
                    )
                    driverRef.updateChildren(map as Map<String, String>)

                    getDriverLocation()
                    request.setText("Looking for Driver Location....")
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
                if (dataSnapshot.exists()) {
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

                    request.text = if (distance < 100) "Driver's Here" else "Driver Found: ".plus(distance.toString())

                    mDriverMarker?.remove()
                    mDriverMarker = mMap.addMarker(MarkerOptions().position(driverLatLng).title("your driver").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car)))
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })

    }
}