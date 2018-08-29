package com.team.eddie.uber_alles.ui.map

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
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
import kotlinx.android.synthetic.main.activity_map_driver.*

class DriverMapActivity : GenericMapActivity() {

    private var status = 0

    private var customerId = ""
    private var destination: String? = null
    private var destinationLatLng: LatLng? = null
    private var pickupLatLng: LatLng? = null
    private var rideDistance: Float = 0.toFloat()

    private var isLoggingOut: Boolean = false;

    companion object {
        fun getLaunchIntent(from: Context) = Intent(from, DriverMapActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_map_driver)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.driver_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        logout.setOnClickListener {
            if(customerId.isBlank()) {
                isLoggingOut = true
                disconnectDriver()
                SaveSharedPreference.cleanAll(applicationContext)
                FirebaseAuth.getInstance().signOut()
                startActivity(WelcomeActivity.getLaunchIntent(this))
            }
            else
                Toast.makeText(this, "Ride must be ended before you can logout", Toast.LENGTH_SHORT).show()
        }

        getAssignedCustomer()
    }

    private fun getAssignedCustomer() {
        val driverId = FirebaseAuth.getInstance().currentUser!!.uid
        val assignedCustomerRef = FirebaseDatabase.getInstance().reference.child("Users").child("Drivers").child(driverId).child("customerRequest").child("customerRideId")
        assignedCustomerRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    status = 1
                    customerId = dataSnapshot.value!!.toString()
                    getAssignedCustomerPickupLocation()
                }
                else{
                    customerId = ""
                    pickupMarker?.remove()
                    assignedCustomerPickupLocationRef?.removeEventListener(assignedCustomerPickupLocationRefListener!!)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private var pickupMarker: Marker? = null
    private var assignedCustomerPickupLocationRef: DatabaseReference? = null
    private var assignedCustomerPickupLocationRefListener: ValueEventListener? = null

    private fun getAssignedCustomerPickupLocation() {
        assignedCustomerPickupLocationRef = FirebaseDatabase.getInstance().reference.child("customerRequest").child(customerId).child("l")
        assignedCustomerPickupLocationRefListener = assignedCustomerPickupLocationRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && customerId != "") {
                    val map: List<*> = dataSnapshot.value as List<*>

                    val locationLat: Double = if (map[0] == null) 0.0 else map[0].toString().toDouble()
                    val locationLng: Double = if (map[1] == null) 0.0 else map[1].toString().toDouble()

                    pickupLatLng = LatLng(locationLat, locationLng)
                    pickupMarker = mMap.addMarker(MarkerOptions().position(pickupLatLng!!).title("pickup location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)))
//                    getRouteToMarker(pickupLatLng)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
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

            if (customerId.isBlank()) {
                geoFireWorking.removeLocation(userId)
                geoFireAvailable.setLocation(userId, GeoLocation(location.latitude, location.longitude))
            } else {
                geoFireAvailable.removeLocation(userId)
                geoFireWorking.setLocation(userId, GeoLocation(location.latitude, location.longitude))
            }
        }
    }

    private fun disconnectDriver(){
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable")
        val refWorking = FirebaseDatabase.getInstance().getReference("driversWorking")
        val geoFireAvailable = GeoFire(refAvailable)
        val geoFireWorking = GeoFire(refWorking)

        geoFireAvailable.removeLocation(userId)
    }

    override fun onStop() {
        super.onStop()
        if(!isLoggingOut)
            disconnectDriver()
    }
}
