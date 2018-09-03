package com.team.eddie.uber_alles.utils

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


object FirebaseHelper {

    private const val ALL_USERS: String = "All_Users";

    const val USERS: String = "Users"
    const val DRIVERS: String = "Drivers"
    const val CUSTOMERS: String = "Customers"

    const val EMAIL: String = "email"
    const val PASSWORD: String = "password"
    const val USERNAME: String = "username"
    const val IS_DRIVER: String = "is_driver"

    private const val RATING: String = "rating"
    private const val HISTORY: String = "history"
    private const val PICKUP: String = "pickup"

    private const val CUSTOMER_REQ: String = "customerRequest";
    private const val DRIVERS_WORKING: String = "driversWorking";
    private const val DRIVERS_AVAILABLE: String = "driversAvailable";
    private const val CUSTOMER_RIDE_ID: String = "customerRideId";


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
}
