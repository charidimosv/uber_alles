package com.team.eddie.uber_alles.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference


object FirebaseHelper {

    private const val ALL_USERS: String = "All_Users";

    private const val USERS: String = "Users"
    private const val DRIVERS: String = "Drivers"
    private const val CUSTOMERS: String = "Customers"

    const val NAME: String = "name"
    const val PHONE: String = "phone"
    const val USERNAME: String = "username"
    const val EMAIL: String = "email"
    const val PASSWORD: String = "password"
    const val IS_DRIVER: String = "is_driver"
    const val PROFILE_IMG_URL: String = "profileImageUrl"
    const val CAR: String = "car"

    const val DESTINATION: String = "destination"
    const val DESTINATION_LAT: String = "destinationLat"
    const val DESTINATION_LOT: String = "destinationLng"
    const val CUSTOMER_RIDE_ID: String = "customerRideId";

    private const val INFO: String = "info"
    private const val RATING: String = "rating"
    private const val HISTORY: String = "history"
    private const val PICKUP: String = "pickup"

    private const val CUSTOMER_REQ: String = "customerRequest";
    private const val DRIVERS_WORKING: String = "driversWorking";
    private const val DRIVERS_AVAILABLE: String = "driversAvailable";

    private const val PROFILE_IMGS: String = "profile_images";


    // general
    private fun getReference(): DatabaseReference {
        return FirebaseDatabase.getInstance().reference
    }

    // generic user
    fun getUser(userId: String): DatabaseReference {
        return getReference().child(ALL_USERS).child(userId)
    }

    fun getUserIsDriver(userId: String): DatabaseReference {
        return getUser(userId).child(IS_DRIVER)
    }

    // customer
    fun getCustomer(customerId: String): DatabaseReference {
        return getReference().child(USERS).child(CUSTOMERS).child(customerId)
    }

    fun getCustomerRating(customerId: String): DatabaseReference {
        return getCustomer(customerId).child(RATING)
    }

    fun getCustomerInfo(customerId: String): DatabaseReference {
        return getCustomer(customerId).child(INFO)
    }

    fun getCustomerHistory(customerId: String): DatabaseReference {
        return getCustomer(customerId).child(HISTORY)
    }

    // driver
    fun getDriver(driverID: String): DatabaseReference {
        return getReference().child(USERS).child(DRIVERS).child(driverID)
    }

    fun getDriverRating(driverID: String): DatabaseReference {
        return getDriver(driverID).child(RATING)
    }

    fun getDriverInfo(driverID: String): DatabaseReference {
        return getDriver(driverID).child(INFO)
    }

    fun getDriverHistory(driverID: String): DatabaseReference {
        return getDriver(driverID).child(HISTORY)
    }

    fun getDriverCustomerReq(driverID: String): DatabaseReference {
        return getDriver(driverID).child(CUSTOMER_REQ)
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

    // storage
    fun getProfileImages(userId: String): StorageReference {
        return FirebaseStorage.getInstance().reference.child(PROFILE_IMGS).child(userId)
    }

    // auth
    fun getUserId(): String {
        return FirebaseAuth.getInstance().currentUser!!.uid
    }
}
