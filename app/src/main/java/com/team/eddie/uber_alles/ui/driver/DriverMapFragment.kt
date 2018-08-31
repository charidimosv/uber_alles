package com.team.eddie.uber_alles.ui.driver

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.databinding.FragmentDriverMapBinding
import com.team.eddie.uber_alles.ui.GenericMapFragment


class DriverMapFragment : GenericMapFragment() {

    lateinit var binding: FragmentDriverMapBinding

    private var status = 0

    private var customerId = ""
    private var destination: String? = null
    private var destinationLatLng: LatLng? = null
    private var pickupLatLng: LatLng? = null
    private var rideDistance: Float = 0.toFloat()

    private var isLoggingOut: Boolean = false;

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

        getAssignedCustomer()

        return binding.root
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
                } else {
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
        if (activity!!.applicationContext != null && location != null) {

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

    private fun disconnectDriver() {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable")
        val refWorking = FirebaseDatabase.getInstance().getReference("driversWorking")
        val geoFireAvailable = GeoFire(refAvailable)
        val geoFireWorking = GeoFire(refWorking)

        geoFireAvailable.removeLocation(userId)
    }

    override fun onStop() {
        super.onStop()
        if (!isLoggingOut)
            disconnectDriver()
    }
}
