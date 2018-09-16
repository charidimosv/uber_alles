package com.team.eddie.uber_alles.utils.firebase

import android.location.Location

class Request(
        var requestId: String = "",

        var customerId: String = "",
        var driverId: String = "",

        var pickupLocation: RequestLocation? = null,
        var pickupTime: Long = 0,
        var requestDate: String = ""
) {
    var destinationList: ArrayList<RequestLocation>? = null

    constructor(customerId: String = "",
                driverId: String = "",
                pickupLocation: Location,
                pickupTime: Long = 0,
                requestDate: String = "",
                locationList: List<Location>)
            : this(customerId = customerId, driverId = driverId, pickupTime = pickupTime, requestDate = requestDate) {

        this.pickupLocation = RequestLocation(pickupLocation.latitude, pickupLocation.longitude)

        if (!locationList.isEmpty()) destinationList = ArrayList()
        for (location in locationList)
            destinationList?.add(RequestLocation(location.latitude, location.longitude))
    }

}

class RequestLocation(
        var lat: Double = 0.0,
        var lng: Double = 0.0
)
