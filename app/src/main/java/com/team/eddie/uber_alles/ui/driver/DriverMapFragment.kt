package com.team.eddie.uber_alles.ui.driver

import android.location.Location
import android.os.Bundle
import android.util.ArraySet
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
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
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
import com.team.eddie.uber_alles.databinding.FragmentDriverMapBinding
import com.team.eddie.uber_alles.ui.ActivityHelper
import com.team.eddie.uber_alles.ui.generic.GenericMapFragment
import com.team.eddie.uber_alles.utils.SaveSharedPreference
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import com.team.eddie.uber_alles.utils.firebase.Request
import com.team.eddie.uber_alles.utils.firebase.UserInfo
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*

private const val DEFAULT_SEARCH_RADIUS: Double = 5555000.0

enum class DriverStatus {
    Free,
    ToCustomer,
    ToDestination
}

open class DriverMapFragment : GenericMapFragment() {

    /*
    ----------------------------------
    UI
    ----------------------------------
    */

    private lateinit var binding: FragmentDriverMapBinding

    private var status: DriverStatus = DriverStatus.Free

    private lateinit var mCustomerInfo: LinearLayout

    private lateinit var mCustomerProfileImage: ImageView

    private lateinit var mCustomerName: TextView
    private lateinit var mCustomerPhone: TextView
    private lateinit var mCustomerDestination: TextView
    private lateinit var mRatingBar: RatingBar
    private lateinit var ratingTextLayout: TextInputLayout
    private lateinit var mRatingText: TextInputEditText
    private lateinit var mRatingButton: MaterialButton
    private lateinit var mRatingAvg: TextView

    /*
    ----------------------------------
    OTHER
    ----------------------------------
    */

    private var customerFoundId: String? = null

    private val destinationMarkerList: ArrayList<Marker> = ArrayList()
    private val destinationLatLngList: ArrayList<LatLng> = ArrayList()

    private var pickupTime: Long? = null
    private var pickupLatLng: LatLng? = null
    private var pickupMarker: Marker? = null

    private var rideDistance: Float = 0.toFloat()

    private var assignedCustomerPickupLocationRef: DatabaseReference? = null
    private var assignedCustomerPickupLocationRefListener: ValueEventListener? = null

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

        mCustomerInfo = binding.customerInfo
        mCustomerProfileImage = binding.customerProfileImage

        mCustomerName = binding.customerName
        mCustomerPhone = binding.customerPhone
        mCustomerDestination = binding.customerDestination

        mRatingBar = binding.ratingBar
        mRatingText = binding.ratingText
        ratingTextLayout = binding.ratingTextLayout
        mRatingButton = binding.ratingButton
        mRatingAvg = binding.ratingAvg


        binding.rideStatus.setOnClickListener {
            if (status == DriverStatus.Free) {
                currentRequest?.run { acceptCustomerRequest(this) }
            } else if (status == DriverStatus.ToCustomer) {
                status = DriverStatus.ToDestination

                erasePolylines()

                if (!destinationLatLngList.isEmpty()) {
                    for (latLng in destinationLatLngList) {
                        val destinationMarker = mMap.addMarker(MarkerOptions()
                                .position(latLng)
                                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)))
                        destinationMarkerList.add(destinationMarker)
                    }
                    getRouteToMarker(LatLng(mLastLocation!!.latitude, mLastLocation!!.longitude), destinationLatLngList)
                }

                binding.rideStatus.text = "Drive completed"
                pickupTime = getCurrentTimestamp()

                binding.callCustomer.visibility = View.GONE
                binding.chatCustomer.visibility = View.GONE

            } else if (status == DriverStatus.ToDestination) {
                if (currentRequest != null) {
                    FirebaseHelper.completeRequest(currentRequest!!)
                    recordRide()
                }

                endRideRequest()
            }
        }

        mRatingButton.setOnClickListener {
            val ratingRef = FirebaseHelper.getUserRating(customerFoundId!!!!)
            val ratingRefId = ratingRef.push().key

            val map = hashMapOf<String, Any?>("value" to mRatingBar.rating/*,"comment" to mRatingText*/)
            ratingRef.child(ratingRefId!!).updateChildren(map)

            clearCustomersInfo()
        }

        binding.chatCustomer.setOnClickListener {
            val direction = DriverMapFragmentDirections.actionDriverMapFragmentToChatFragment()
            it.findNavController().navigate(direction)
        }

        getActiveRequest()

        return binding.root
    }

    override fun onLocationChanged(location: Location?) {
        super.onLocationChanged(location)

        if (customerFoundId!!.isBlank() && location != null)
            FirebaseHelper.addDriverAvailable(currentUserId, GeoLocation(location.latitude, location.longitude))
        else
            FirebaseHelper.removeDriverAvailable(currentUserId)
    }

    override fun getActiveRequest() {
        activeRequestRef = FirebaseHelper.getUserActiveRequest(currentUserId)
        activeRequestListener = activeRequestRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    getRequestInfo(dataSnapshot.value.toString())
                } else {
                    completedRide = true
                    endRideRequest()

                    findNextCustomer()
                }
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

                    startRideUI()

                    customerFoundId = currentRequest?.customerId

                    getAssignedCustomerInfo()
                    getAssignedCustomerLocation()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun getAssignedCustomerInfo() {
        // TODO uncomment later
        // mCustomerInfo.visibility = View.VISIBLE

        val mCustomerDatabase = FirebaseHelper.getUserInfo(customerFoundId!!)
        mCustomerDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                    val userInfo = dataSnapshot.getValue(UserInfo::class.java)
                    userInfo ?: return

                    userInfo.name.let { mCustomerName.text = it }
                    userInfo.phone.let { mCustomerPhone.text = it }
                    userInfo.imageUrl?.let { ActivityHelper.bindImageFromUrl(mCustomerProfileImage, it) }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })

        val mCustomerRating = FirebaseHelper.getUserRating(customerFoundId!!)
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
                mRatingAvg.text = "Average Rating: " + df.format(ratingsAvg).toString() + "/5"

            }

        })

        SaveSharedPreference.setChatSender(applicationContext, currentUserId)
        SaveSharedPreference.setChatReceiver(applicationContext, customerFoundId!!)
        newIncomeMessageRef = FirebaseHelper.getMessageUsers(currentUserId + "_to_" + customerFoundId!!)

        binding.callCustomer.visibility = View.VISIBLE
        binding.chatCustomer.visibility = View.VISIBLE

        newIncomeMessageListener = newIncomeMessageRef?.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) binding.chatCustomer.text = getString(R.string.message_excl)
            }

        })
    }

    private fun getAssignedCustomerLocation() {
        customerLocationRef = FirebaseHelper.getUserLocation(customerFoundId!!)
        customerLocationListener = customerLocationRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && SaveSharedPreference.getActiveRequest(applicationContext)) {
                    val map = dataSnapshot.value as List<Any?>

                    val locationLat: Double = map[0]?.toString()?.toDouble() ?: 0.0
                    val locationLng: Double = map[1]?.toString()?.toDouble() ?: 0.0

                    val latLng = LatLng(locationLat, locationLng)

                    mCustomerMarker?.remove()
                    mCustomerMarker = mMap.addMarker(MarkerOptions().position(latLng).title(getString(R.string.your_driver)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car)))
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
                    pendingRequestQuery?.removeAllListeners()
                    getRequestInfo(key)
                }
            }

            override fun onKeyExited(key: String) {

            }

            override fun onKeyMoved(key: String, location: GeoLocation) {

            }

            override fun onGeoQueryReady() {
                if (customerFoundId == null && searchCustomersAround)
                    findNextCustomer()
            }

            override fun onGeoQueryError(error: DatabaseError) {

            }
        })
    }

    // TODO - kapoi prepei na mpei auto
    private fun acceptCustomerRequest(request: Request) {
        status = DriverStatus.ToCustomer

        pickupLatLng = LatLng(request.pickupLocation?.lat!!, request.pickupLocation?.lng!!)
        pickupMarker = mMap.addMarker(MarkerOptions()
                .position(pickupLatLng!!)
                .title("Pickup Location")
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)))
        getRouteToMarker(pickupLatLng)

        for (reqLocation in request.destinationList!!)
            destinationLatLngList.add(LatLng(reqLocation.lat, reqLocation.lng))

        FirebaseHelper.acceptRequest(request)
    }

    override fun startRideRequest() {
    }

    override fun endRideRequest() {
        binding.rideStatus.text = getString(R.string.picked_customer)

        FirebaseHelper.removeRequest(currentRequest!!)

        assignedCustomerPickupLocationRef?.removeEventListener(assignedCustomerPickupLocationRefListener!!)
        newIncomeMessageRef?.removeEventListener(newIncomeMessageListener!!)

        startFreshUI()

        if (status == DriverStatus.ToDestination) {
            mRatingBar.rating = 0.toFloat()
            mRatingBar.setIsIndicator(false)
            mRatingBar.numStars = 5
            mRatingAvg.visibility = View.GONE
            mRatingButton.visibility = View.VISIBLE
            ratingTextLayout.visibility = View.VISIBLE
        } else
            clearCustomersInfo()
    }

    private fun clearCustomersInfo() {
        customerFoundId = null

        status = DriverStatus.Free
        mRatingBar.rating = 0.toFloat()
        mRatingAvg.text = ""
        //mRatingText = null
        mRatingButton.visibility = View.GONE
        ratingTextLayout.visibility = View.GONE

        mRatingBar.rating = 0.toFloat()
        mRatingBar.setIsIndicator(false)
        mRatingBar.numStars = 1
        mRatingAvg.text = ""
        mRatingAvg.visibility = View.VISIBLE
        mCustomerInfo.visibility = View.GONE
        mCustomerName.text = ""
        mCustomerPhone.text = ""
        mCustomerDestination.text = "Destination: --"
        mCustomerProfileImage.setImageResource(R.mipmap.ic_default_user)

    }

    private fun recordRide() {
        // TODO - hackie-hackie-hackie
        val listSize = currentRequest?.destinationList?.size ?: 0
        val lastRequestLocation = currentRequest?.destinationList?.get(listSize - 1)
        FirebaseHelper.addHistoryForDriverCustomer(currentUserId, customerFoundId!!, pickupTime, getCurrentTimestamp(), lastRequestLocation?.locName, rideDistance,
                pickupLatLng?.latitude, pickupLatLng?.longitude, lastRequestLocation?.lat, lastRequestLocation?.lng)
    }

    private fun clearDestinationInfo() {
        for (marker in destinationMarkerList) marker.remove()
        destinationMarkerList.clear()
        destinationLatLngList.clear()
    }

    override fun startFreshUI() {
        pickupMarker?.remove()
        clearDestinationInfo()

        erasePolylines()
    }

    override fun startRideUI() {
    }

}
