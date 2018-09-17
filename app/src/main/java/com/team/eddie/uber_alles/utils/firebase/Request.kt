package com.team.eddie.uber_alles.utils.firebase

import android.location.Location
import com.google.android.gms.location.places.Place
import com.team.eddie.uber_alles.utils.Status

class Request(
        var requestId: String = "",

        var customerId: String = "",
        var driverId: String = "",

        var pickupLocation: RequestLocation? = null,
        var pickupTime: Long = 0,
        var requestDate: String = "",

        var arrivingTime: Long? = 0,
        var distance: Float = 0F,

        var amount: Double = 0.0,
        var payByCard: Boolean = false,

        var status: Status = Status.Pending
) {
    var destinationList: ArrayList<RequestLocation>? = null

    constructor(customerId: String = "",
                pickupLocation: Location,
                locationList: ArrayList<Place>,
                requestDate: String = "")
            : this(customerId = customerId,
            driverId = "",
            requestDate = requestDate,
            status = Status.Pending) {

        this.pickupLocation = RequestLocation("", pickupLocation.latitude, pickupLocation.longitude)

        if (!locationList.isEmpty()) destinationList = ArrayList()
        for (place in locationList)
            destinationList?.add(RequestLocation(place.name.toString(), place.latLng.latitude, place.latLng.longitude))
    }
}

class RequestLocation(
        var locName: String = "",
        var lat: Double = 0.0,
        var lng: Double = 0.0
)
