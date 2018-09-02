package com.team.eddie.uber_alles.ui.driver

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.databinding.FragmentDriverMapBinding
import com.team.eddie.uber_alles.ui.GenericMapFragment
import com.directions.route.*
import com.google.android.gms.maps.model.*
import java.util.ArrayList
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import android.widget.TextView
import android.widget.LinearLayout
import com.bumptech.glide.Glide


class DriverMapFragment : GenericMapFragment(), RoutingListener {

    lateinit var binding: FragmentDriverMapBinding

    private var status = 0

    private var customerId = ""
    private var destination: String? = null
    private var destinationLatLng: LatLng? = null
    private var pickupLatLng: LatLng? = null
    private var rideDistance: Float = 0.toFloat()

    private var isLoggingOut: Boolean = false

    private var mCustomerInfo: LinearLayout? = null

    private var mCustomerProfileImage: ImageView? = null

    private var mCustomerName: TextView? = null
    private var mCustomerPhone: TextView? = null
    private var mCustomerDestination: TextView? = null

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

        binding.rideStatus.setOnClickListener{

            if(status == 1){
                status=2
                erasePolylines()
                if(destinationLatLng?.latitude != 0.0 && destinationLatLng?.longitude != 0.0){
                    getRouteToMarker(destinationLatLng);
                }
                binding.rideStatus.text = "Drive completed"
            }
            else{
               // recordRide()
                endRide()
            }
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
        val driverId = FirebaseAuth.getInstance().currentUser!!.uid
        val assignedCustomerRef = FirebaseDatabase.getInstance().reference.child("Users").child("Drivers").child(driverId).child("customerRequest").child("customerRideId")
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
        assignedCustomerPickupLocationRef = FirebaseDatabase.getInstance().reference.child("customerRequest").child(customerId).child("l")
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

    private fun getAssignedCustomerDestination(){
        val driverId = FirebaseAuth.getInstance().currentUser?.uid
        val assignedCustomerRequestRef = FirebaseDatabase.getInstance().reference.child("Users").child("Drivers").child(driverId!!).child("customerRequest")

        assignedCustomerRequestRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.exists()) {
                    val map: Map<String, Object> = dataSnapshot.value as Map<String, Object>

                    if(map["destination"] !=null){
                        destination = map["destination"].toString()
                        mCustomerDestination?.text = "Destination: $destination"
                    }
                    else
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
        val mCustomerDatabase = FirebaseDatabase.getInstance().reference.child("Users").child("Customers").child(customerId)
        mCustomerDatabase?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                    val map = dataSnapshot.value as Map<String, Any>?
                    if (map!!["name"] != null)
                        mCustomerName?.text = map["name"].toString()

                    if (map["phone"] != null)
                        mCustomerPhone?.text = map["phone"].toString()

                    if (map["profileImageUrl"] != null)
                        Glide.with(mCustomerInfo!!).load(map["profileImageUrl"].toString()).into(mCustomerProfileImage!!)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun endRide(){
        binding.rideStatus.text = "Picked Customer"

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val driverRef = FirebaseDatabase.getInstance().reference.child("Users").child("Drivers").child(userId!!).child("customerRequest")
        driverRef.removeValue();

        val ref = FirebaseDatabase.getInstance().getReference("customerRequest")
        val geoFire = GeoFire(ref)
        geoFire.removeLocation(customerId)

        pickupMarker?.remove()
        assignedCustomerPickupLocationRef?.removeEventListener(assignedCustomerPickupLocationRefListener!!)

        customerId = ""
        rideDistance = 0.toFloat()
        pickupMarker?.remove()
        assignedCustomerPickupLocationRef?.removeEventListener(assignedCustomerPickupLocationRefListener!!)
        mCustomerInfo?.visibility = View.GONE
        mCustomerName?.text = ""
        mCustomerPhone?.text = ""
        mCustomerDestination?.text = "Destination: --"
        mCustomerProfileImage?.setImageResource(R.mipmap.ic_default_user)
        erasePolylines()

    }


    private fun getRouteToMarker(pickupLatLng: LatLng?) {
        if (pickupLatLng != null && mLastLocation != null) {
            val routing = Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(false)
                    .waypoints(LatLng(mLastLocation!!.latitude, mLastLocation!!.longitude), pickupLatLng)
                    .build()
            routing.execute()
        }
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

    private var polylines = arrayListOf<Polyline>()
    private val COLORS = intArrayOf(R.color.primary_dark_material_light)
    override fun onRoutingCancelled() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onRoutingStart() {
    }

    override fun onRoutingFailure(p0: RouteException?) {
        if(p0 != null)
            Toast.makeText(activity!!, "Error: " + p0.message, Toast.LENGTH_LONG).show()
        else
            Toast.makeText(activity!!, "Something went wrong, Try again", Toast.LENGTH_SHORT).show()
    }

    override fun onRoutingSuccess(route: ArrayList<Route>?, p1: Int) {
        erasePolylines()
        //add route(s) to the map.
        for (i in 0 until route?.size!!) {

            //In case of more than 5 alternative routes
            val colorIndex = i % COLORS.size

            val polyOptions = PolylineOptions()
            polyOptions.color(resources.getColor(COLORS[colorIndex], resources.newTheme()))
            polyOptions.width((10 + i * 3).toFloat())
            polyOptions.addAll(route[i].points)
            val polyline = mMap.addPolyline(polyOptions)
            polylines.add(polyline)

            Toast.makeText(activity!!, "Route " + (i + 1) + ": distance - " + route.get(i).distanceValue + ": duration - " + route.get(i).durationValue, Toast.LENGTH_SHORT).show()
        }
    }

    private fun erasePolylines() {
        for (line in polylines)
            line.remove()
    }

}