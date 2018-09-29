package com.team.eddie.uber_alles.utils.firebase

import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.team.eddie.uber_alles.utils.Status
import java.util.concurrent.CountDownLatch


object FirebaseHelper {

/*
------------
TABLES
------------
        User
            userID
                UserInfo
                    email
                    driverId
                    name
                    password
                    phone
                    username
                CarList
                    carID
                HistoryList
                RatingList

        Car
            carID
                Car

        History
            userID
                HistoryItem

        Request
            requestID

        Message
            userID_to_userID
                meesageID
                    message
                    userID

------------
HELPERS
------------

        DriversAvailable
            userID
                loc
 */

    private const val USER: String = "User"
    private const val USER_INFO: String = "UserInfo"
    private const val PAYMENT_INFO: String = "PaymentInfo"
    private const val CAR_LIST: String = "CarList"
    private const val DEFAULT_CAR: String = "DefaultCar"
    private const val REQUEST_LIST: String = "RequestList"
    private const val RATING_LIST: String = "RatingList"

    private const val CAR: String = "Car"
    private const val RATING: String = "Rating"
    private const val MESSAGE: String = "Message"
    private const val REQUEST: String = "Request"

    private const val LOCATION: String = "location"

    private const val PENDING_REQUEST: String = "PendingRequest"
    private const val ACTIVE_REQUEST: String = "activeRequest"

    // user info
    const val NAME: String = "name"
    const val PHONE: String = "phone"
    const val USERNAME: String = "username"
    const val EMAIL: String = "email"
    const val PASSWORD: String = "password"
    const val IS_DRIVER: String = "driverId"
    const val DESTINATION: String = "destination"


    // history related
    const val IMG_URL: String = "imageUrl"
    private const val PROFILE_IMGS: String = "profile_images"
    private const val CAR_IMGS: String = "car_images"

    const val DESTINATION_LAT: String = "destinationLat"
    const val DESTINATION_LOT: String = "destinationLng"
    const val CUSTOMER_RIDE_ID: String = "customerRideId"

    const val RATING_LEG: String = "rating"
    private const val NEW_MESSAGE: String = "newMessagePushed"
    private const val PICKUP: String = "pickup"

    private const val CUSTOMER_REQ: String = "customerRequest"
    private const val DRIVERS_WORKING: String = "driversWorking"
    private const val DRIVERS_AVAILABLE: String = "driversAvailable"

    private lateinit var snapshot: DataSnapshot

    /*
    ----------------------------------
    GENERAL
    ----------------------------------
    */

    private fun getReference(): DatabaseReference {
        return FirebaseDatabase.getInstance().reference
    }

    /*
    ----------------------------------
    USER
    ----------------------------------
    */

    private fun getUser(userId: String): DatabaseReference {
        return getReference().child(USER).child(userId)
    }

    fun getUserInfo(userId: String): DatabaseReference {
        return getUser(userId).child(USER_INFO)
    }

    fun getUserCar(userId: String): DatabaseReference {
        return getUser(userId).child(CAR_LIST)
    }

    fun getUserDefaultCar(userId: String): DatabaseReference {
        return getUser(userId).child(DEFAULT_CAR)
    }

    fun getUserCarKey(userId: String, carId: String): DatabaseReference {
        return getUserCar(userId).child(carId)
    }

    fun getUserRequestList(userId: String): DatabaseReference {
        return getUser(userId).child(REQUEST_LIST)
    }

    fun getUserRating(userId: String): DatabaseReference {
        return getUser(userId).child(RATING_LIST)
    }

    fun getUserActiveRequest(userId: String): DatabaseReference {
        return getUser(userId).child(ACTIVE_REQUEST)
    }

    fun getUserIsDriver(userId: String): DatabaseReference {
        return getUserInfo(userId).child(IS_DRIVER)
    }

    fun cleanUser(userId: String) {
        getUser(userId).removeValue()
    }

    fun setUserLocation(userId: String, location: GeoLocation) {
        val geoFireAvailable = GeoFire(getUser(userId))
        geoFireAvailable.setLocation(LOCATION, location)
    }

    fun getUserLocation(userId: String): DatabaseReference {
        return getUser(userId).child(LOCATION).child("l")
    }

    fun getUserPaymentInfo(userId: String): DatabaseReference {
        return getUser(userId).child(PAYMENT_INFO)
    }

    /*
    ----------------------------------
    REQUEST
    ----------------------------------
    */

    fun getRequest(): DatabaseReference {
        return getReference().child(REQUEST)
    }

    fun getRequestKey(requestId: String): DatabaseReference {
        return getRequest().child(requestId)
    }

    fun getPendingRequest(): DatabaseReference {
        return getReference().child(PENDING_REQUEST)
    }

    private fun getPendingRequestKey(requestId: String): DatabaseReference {
        return getPendingRequest().child(requestId)
    }

    fun createRequest(request: Request) {
        val requestRef = getRequest()
        val requestId = requestRef.push().key!!

        request.requestId = requestId
        requestRef.child(requestId).setValue(request)

        val pickupLocation = GeoLocation(request.pickupLocation?.latLng!!.latitude, request.pickupLocation?.latLng!!.longitude)
        val geoPendingRequest = GeoFire(getPendingRequest())
        geoPendingRequest.setLocation(requestId, pickupLocation)
    }

    fun updateRequest(request: Request) {
        val requestRef = getRequestKey(request.requestId)
        requestRef.setValue(request)
    }

    fun removeRequest(request: Request) {
        if (request.customerId != "") {
            val customerARRef = getUserActiveRequest(request.customerId)
            customerARRef.setValue(null)
        }

        if (request.driverId != "") {
            val driverARRef = getUserActiveRequest(request.driverId)
            driverARRef.setValue(null)
        }

        val pendingRequestRef = getPendingRequestKey(request.requestId)
        pendingRequestRef.setValue(null)

        val requestRef = getRequestKey(request.requestId)
        requestRef.setValue(null)
    }

    fun acceptRequest(request: Request) {
        val requestId = request.requestId

        val customerARRef = getUserActiveRequest(request.customerId)
        customerARRef.setValue(requestId)

        val driverARRef = getUserActiveRequest(request.driverId)
        driverARRef.setValue(requestId)

        val pendingRequestRef = getPendingRequestKey(requestId)
        pendingRequestRef.setValue(null)

        val requestRef = getRequestKey(request.requestId)
        requestRef.setValue(request)
    }

    fun completeRequest(request: Request) {
        val customerARRef = getUserActiveRequest(request.customerId)
        customerARRef.setValue(null)

        val driverARRef = getUserActiveRequest(request.driverId)
        driverARRef.setValue(null)

        val requestRef = getRequestKey(request.requestId)
        request.status = Status.Done
        requestRef.setValue(request)

        val driverRequestRef = getUserRequestList(request.driverId)
        driverRequestRef.child(request.requestId).setValue(true)

        val customerRequestRef = getUserRequestList(request.customerId)
        customerRequestRef.child(request.requestId).setValue(true)
    }

    /*
    ----------------------------------
    DRIVER
    ----------------------------------
    */

    fun getDriverCustomerReq(driverID: String): DatabaseReference {
        return getUser(driverID).child(CUSTOMER_REQ)
    }

    fun getDriverCustomerRide(driverID: String): DatabaseReference {
        return getDriverCustomerReq(driverID).child(CUSTOMER_RIDE_ID)
    }

    fun getDriverCustomerReqPickup(driverID: String): DatabaseReference {
        return getDriverCustomerReq(driverID).child(PICKUP)
    }

    /*
    ----------------------------------
    HELPER
    ----------------------------------
    */

    fun getDriversAvailable(): DatabaseReference {
        return getReference().child(DRIVERS_AVAILABLE)
    }

    fun addDriverAvailable(driverID: String, location: GeoLocation) {
        val geoFireAvailable = GeoFire(getDriversAvailable())
        geoFireAvailable.setLocation(driverID, location)
    }

    fun removeDriverAvailable(driverID: String) {
        val geoFireAvailable = GeoFire(getDriversAvailable())
        geoFireAvailable.removeLocation(driverID)
    }

    fun getDriversWorking(): DatabaseReference {
        return getReference().child(DRIVERS_WORKING)
    }

    fun getDriversWorkingLocation(driverID: String): DatabaseReference {
        return getDriversWorking().child(driverID).child("l")
    }

    fun getCustomerRequest(): DatabaseReference {
        return getReference().child(CUSTOMER_REQ)
    }

    fun getCustomerRequestLocation(customerId: String): DatabaseReference {
        return getCustomerRequest().child(customerId).child("l")
    }

    /*
    ----------------------------------
    CAR
    ----------------------------------
    */

    fun getCar(): DatabaseReference {
        return getReference().child(CAR)
    }

    fun getCarKey(carId: String): DatabaseReference {
        return getCar().child(carId)
    }

    fun createCarForDriver(driverID: String): String {
        val driverCarRef = getUserCar(driverID)
        val carRef = getCar()

        val carId = carRef.push().key
        driverCarRef.child(carId!!).setValue(true)

        return carId
    }

    fun updateDefaultCarForDriver(driverID: String, carId: String) {
        val driverDefaultCarRef = getUserDefaultCar(driverID)
        var previousDefault: String? = ""
        driverDefaultCarRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    previousDefault = dataSnapshot.value.toString()
                    if (previousDefault != carId) {
                        val previousCarRef = getCar().child(previousDefault!!)
                        val map = hashMapOf("defaultCar" to "false")
                        previousCarRef.updateChildren(map as Map<String, Any>)
                    }

                }
                driverDefaultCarRef.setValue(carId)
            }

        })
    }

    fun deleteCar(carId: String, driverID: String) {
        val driverCarRef = getUserCarKey(driverID, carId)
        val carRef = getCarKey(carId)
        val defaultCarRef = getUserDefaultCar(driverID)
        defaultCarRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.value.toString() == carId)
                    defaultCarRef.setValue(null)
            }

        })

        driverCarRef.setValue(null)
        carRef.setValue(null)
    }

    /*
    ----------------------------------
    MESSAGE
    ----------------------------------
    */

    fun getMessage(): DatabaseReference {
        return getReference().child(MESSAGE)
    }

    fun getMessageUsers(users: String): DatabaseReference {
            return getMessage().child(users).child(NEW_MESSAGE)
    }

    /*
    ----------------------------------
    STORAGE
    ----------------------------------
    */

    fun getProfileImages(userId: String): StorageReference {
        return FirebaseStorage.getInstance().reference.child(PROFILE_IMGS).child(userId)
    }

    fun getCarImages(carId: String): StorageReference {
        return FirebaseStorage.getInstance().reference.child(CAR_IMGS).child(carId)
    }

    /*
    ----------------------------------
    AUTH
    ----------------------------------
    */

    fun getUserId(): String {
        return FirebaseAuth.getInstance().currentUser!!.uid
    }

    /*
    ----------------------------------
    MISC
    ----------------------------------
    */

    fun loadSynchronous(databaseReference: DatabaseReference): DataSnapshot? {
        val latch = CountDownLatch(1)
        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                snapshot = dataSnapshot
                latch.countDown()
            }

            override fun onCancelled(dbError: DatabaseError) {
                println("Error loading location")
                latch.countDown()
            }
        }
        databaseReference.addListenerForSingleValueEvent(listener)
        latch.await()
        databaseReference.removeEventListener(listener)
        return snapshot
    }

}
