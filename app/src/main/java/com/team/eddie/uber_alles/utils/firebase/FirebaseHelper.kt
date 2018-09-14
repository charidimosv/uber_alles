package com.team.eddie.uber_alles.utils.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.database.DataSnapshot
import android.R.attr.countDown
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.CountDownLatch


object FirebaseHelper {

    private const val USERS: String = "Users";

    const val NAME: String = "name"
    const val PHONE: String = "phone"
    const val USERNAME: String = "username"
    const val EMAIL: String = "email"
    const val PASSWORD: String = "password"
    const val IS_DRIVER: String = "is_driver"
    const val PROFILE_IMG_URL: String = "profileImageUrl"

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

    // car related
    const val CAR_BRAND: String = "brand"
    const val CAR_MODEL: String = "model"
    const val CAR_PLATE: String = "plate"
    const val CAR_YEAR: String = "year"
    const val CAR_IMG_URL: String = "carImageUrl"

    const val DESTINATION_LAT: String = "destinationLat"
    const val DESTINATION_LOT: String = "destinationLng"
    const val CUSTOMER_RIDE_ID: String = "customerRideId"

    const val RIDE_STATUS: String = "rideStatus"
    const val RIDE_STATUS_REQUEST: String = "request"
    const val RIDE_STATUS_ACCEPTED: String = "accepted"
    const val RIDE_STATUS_RIDE: String = "ride"

    private const val INFO: String = "info"
    const val RATING: String = "rating"
    private const val HISTORY: String = "history"
    const val CAR: String = "car"
    private const val MESSAGE: String = "message"
    private const val NEW_MESSAGE: String = "newMessagePushed"
    private const val PICKUP: String = "pickup"

    private const val CUSTOMER_REQ: String = "customerRequest"
    private const val DRIVERS_WORKING: String = "driversWorking"
    private const val DRIVERS_AVAILABLE: String = "driversAvailable"

    private const val PROFILE_IMGS: String = "profile_images"
    private const val CAR_IMGS: String = "car_images"

    private lateinit var snapshot: DataSnapshot


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

    fun getUserCar(userId: String): DatabaseReference {
        return getUser(userId).child(CAR)
    }

    fun getUserCarKey(userId: String, carId: String): DatabaseReference {
        return getUserCar(userId).child(carId)
    }

    fun cleanUser(userId: String) {
        getUser(userId).removeValue()
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

    // history
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
                DISTANCE to rideDistance)
        historyRef.child(requestId).updateChildren(map)
    }


    // car
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


    // messages
    fun getMessage(): DatabaseReference {
        return getReference().child(MESSAGE)
    }

    fun getMessageUsers(users: String): DatabaseReference {
        return getMessage().child(users).child(NEW_MESSAGE)
    }

    // storage
    fun getProfileImages(userId: String): StorageReference {
        return FirebaseStorage.getInstance().reference.child(PROFILE_IMGS).child(userId)
    }

    fun getCarImages(carId: String): StorageReference {
        return FirebaseStorage.getInstance().reference.child(CAR_IMGS).child(carId)
    }

    // auth
    fun getUserId(): String {
        return FirebaseAuth.getInstance().currentUser!!.uid
    }


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
