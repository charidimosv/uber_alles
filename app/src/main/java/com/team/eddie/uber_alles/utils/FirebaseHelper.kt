package com.team.eddie.uber_alles.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference


object FirebaseHelper {

    private const val USERS: String = "Users";

    const val NAME: String = "name"
    const val PHONE: String = "phone"
    const val USERNAME: String = "username"
    const val EMAIL: String = "email"
    const val PASSWORD: String = "password"
    const val IS_DRIVER: String = "is_driver"
    const val PROFILE_IMG_URL: String = "profileImageUrl"
    const val CAR: String = "car"

    // history related
    const val DRIVER: String = "driver"
    const val CUSTOMER: String = "customer"
    const val PICKUP_TIME: String = "pickupTime"
    const val ARRIVING_TIME: String = "arrivingTime"
    const val DESTINATION: String = "destination"
    const val LOCATION: String = "location"
    const val LOC_FROM_LAT: String = "location/from/lat"
    const val LOC_FROM_LNG: String = "location/from/lng"
    const val LOC_TO_LAT: String = "location/to/lat"
    const val LOC_TO_LNG: String = "location/to/lng"
    const val DISTANCE: String = "distance"
    const val COST: String = "cost"

    const val DESTINATION_LAT: String = "destinationLat"
    const val DESTINATION_LOT: String = "destinationLng"
    const val CUSTOMER_RIDE_ID: String = "customerRideId";

    private const val INFO: String = "info"
    const val RATING: String = "rating"
    private const val HISTORY: String = "history"
    private const val MESSAGE: String = "message"
    private const val PICKUP: String = "pickup"

    private const val CUSTOMER_REQ: String = "customerRequest"
    private const val DRIVERS_WORKING: String = "driversWorking"
    private const val DRIVERS_AVAILABLE: String = "driversAvailable"

    private const val PROFILE_IMGS: String = "profile_images"


    // general
    private fun getReference(): DatabaseReference {
        return FirebaseDatabase.getInstance().reference
    }

    // generic user
    fun getUser(userId: String): DatabaseReference {
        return getReference().child(USERS).child(userId)
    }

    fun getUserInfo(userId: String): DatabaseReference {
        return getUser(userId).child(INFO)
    }

    fun getUserIsDriver(userId: String): DatabaseReference {
        return getUserInfo(userId).child(IS_DRIVER)
    }

    fun getUserRating(userId: String): DatabaseReference {
        return getUser(userId).child(RATING)
    }

    fun getUserHistory(userId: String): DatabaseReference {
        return getUser(userId).child(HISTORY)
    }

    // driver
    fun getDriverCustomerReq(driverID: String): DatabaseReference {
        return getUser(driverID).child(CUSTOMER_REQ)
    }

    fun getDriverCustomerRide(driverID: String): DatabaseReference {
        return getDriverCustomerReq(driverID).child(CUSTOMER_RIDE_ID)
    }

    fun getDriverCustomerReqPickup(driverID: String): DatabaseReference {
        return getDriverCustomerReq(driverID).child(PICKUP)
    }

    // utilities
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

    // misc
    fun getHistory(): DatabaseReference {
        return getReference().child(HISTORY)
    }

    // misc
    fun getMessage(): DatabaseReference {
        return getReference().child(MESSAGE)
    }



    fun getHistoryKey(key: String): DatabaseReference {
        return getHistory().child(key)
    }

    // storage
    fun getProfileImages(userId: String): StorageReference {
        return FirebaseStorage.getInstance().reference.child(PROFILE_IMGS).child(userId)
    }

    // auth
    fun getUserId(): String {
        return FirebaseAuth.getInstance().currentUser!!.uid
    }

    // history
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
        val driverHistoryRef = FirebaseHelper.getUserHistory(driverID)
        val customerHistoryRef = FirebaseHelper.getUserHistory(customerID)
        val historyRef = FirebaseHelper.getHistory()

        val requestId = historyRef.push().key
        driverHistoryRef.child(requestId!!).setValue(true)
        customerHistoryRef.child(requestId).setValue(true)

        val map = hashMapOf<String, Any?>(
                DRIVER to driverID,
                CUSTOMER to customerID,
                PICKUP_TIME to pickupTime,
                ARRIVING_TIME to arrivingTime,
                DESTINATION to destination,
                LOC_FROM_LAT to locFromLat,
                LOC_FROM_LNG to locFromLng,
                LOC_TO_LAT to locToLat,
                LOC_TO_LNG to locToLng,
                DISTANCE to rideDistance
        )
        historyRef.child(requestId).updateChildren(map)
    }
}
