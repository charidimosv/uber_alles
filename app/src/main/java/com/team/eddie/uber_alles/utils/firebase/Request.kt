package com.team.eddie.uber_alles.utils.firebase

import android.location.Location
import com.team.eddie.uber_alles.utils.Status

class Request(
        var requestId: String = "",

        var customerId: String = "",
        var driverId: String = "",

        var pickupLocation: PlaceShort? = null,
        var destinationList: ArrayList<PlaceShort> = ArrayList(),

        var requestDate: String = "",
        var pickupTime: Long = 0,
        var arrivingTime: Long? = 0,

        var distance: Float = 0F,
        var amount: Double = 0.0,

        var payByCard: Boolean = false,

        var status: Status = Status.Pending
) {
    constructor(customerId: String = "",
                pickupLocation: Location,
                placeList: ArrayList<PlaceShort>,
                requestDate: String = "")
            : this(customerId = customerId,
            driverId = "",
            destinationList = placeList,
            requestDate = requestDate) {
        this.pickupLocation = PlaceShort(latLng = LatLngShort(latitude = pickupLocation.latitude, longitude = pickupLocation.longitude))
    }
}

class PlaceShort(
        var id: String = "",
        var name: String = "",
        var address: String = "",
        var latLng: LatLngShort = LatLngShort(0.0, 0.0)
)

class LatLngShort(
        var latitude: Double = 0.0,
        var longitude: Double = 0.0
)
