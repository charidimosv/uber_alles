package com.team.eddie.uber_alles.utils.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
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
                    isDriver
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
    private const val CAR_LIST: String = "CarList"
    private const val HISTORY_LIST: String = "HistoryList"
    private const val RATING_LIST: String = "RatingList"

    private const val CAR: String = "Car"
    private const val HISTORY: String = "History"
    private const val RATING: String = "Rating"
    private const val MESSAGE: String = "Message"

    // user info
    const val NAME: String = "name"
    const val PHONE: String = "phone"
    const val USERNAME: String = "username"
    const val EMAIL: String = "email"
    const val PASSWORD: String = "password"
    const val IS_DRIVER: String = "is_driver"
    const val DESTINATION: String = "destination"

    // history related
    const val IMG_URL: String = "imageUrl"
    private const val PROFILE_IMGS: String = "car_images"
    private const val CAR_IMGS: String = "profile_images"

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

    fun getUserCarKey(userId: String, carId: String): DatabaseReference {
        return getUserCar(userId).child(carId)
    }

    fun getUserHistory(userId: String): DatabaseReference {
        return getUser(userId).child(HISTORY_LIST)
    }

    fun getUserRating(userId: String): DatabaseReference {
        return getUser(userId).child(RATING_LIST)
    }

    fun getUserIsDriver(userId: String): DatabaseReference {
        return getUserInfo(userId).child(IS_DRIVER)
    }

    fun cleanUser(userId: String) {
        getUser(userId).removeValue()
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
        return getReference().child(CUSTOMER_REQ).child(customerId).child("l")
    }

    /*
    ----------------------------------
    HISTORY
    ----------------------------------
    */

    fun getHistory(): DatabaseReference {
        return getReference().child(HISTORY)
    }

    fun getHistoryKey(key: String): DatabaseReference {
        return getHistory().child(key)
    }

    fun addHistoryForDriverCustomer(
            driverID: String,
            customerID: String,
            pickupTime: Long?,
            arrivingTime: Long,
            destination: String?,
            rideDistance: Float,
            locFromLat: Double?,
            locFromLng: Double?,
            locToLat: Double?,
            locToLng: Double?
    ) {
        val driverHistoryRef = getUserHistory(driverID)
        val customerHistoryRef = getUserHistory(customerID)

        val historyRef = getHistory()

        val requestId = historyRef.push().key
        driverHistoryRef.child(requestId!!).setValue(true)
        customerHistoryRef.child(requestId).setValue(true)

        val historyItem = HistoryItem(
                requestId,
                customerID,
                driverID,
                pickupTime,
                arrivingTime,
                destination,
                rideDistance,
                locFromLat, locFromLng,
                locToLat, locToLng)
        historyRef.child(requestId).setValue(historyItem)
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

    fun deleteCar(carId: String, driverID: String) {
        val driverCarRef = getUserCarKey(driverID, carId)
        val carRef = getCarKey(carId)

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
