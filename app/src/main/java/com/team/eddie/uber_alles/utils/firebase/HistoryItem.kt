package com.team.eddie.uber_alles.utils.firebase

class HistoryItem(
        var rideId: String? = null,

        var customer: String? = null,
        var driver: String? = null,

        var pickupTime: Long? = null,
        var arrivingTime: Long? = null,

        var destination: String? = null,
        var distance: Float? = null) {

    var location: HistoryLocationCombo? = null

    constructor(rideId: String? = null,
                customer: String? = null,
                driver: String? = null,
                pickupTime: Long? = null,
                arrivingTime: Long? = null,
                destination: String? = null,
                distance: Float? = null,

                locFromLat: Double?,
                locFromLng: Double?,
                locToLat: Double?,
                locToLng: Double?)
            : this(rideId, customer, driver, pickupTime, arrivingTime, destination, distance) {

        location = HistoryLocationCombo(
                locFromLat, locFromLng,
                locToLat, locToLng)
    }
}


class HistoryLocationCombo(
        var from: HistoryLocationSingle? = null,
        var to: HistoryLocationSingle? = null) {

    constructor (
            locFromLat: Double?,
            locFromLng: Double?,
            locToLat: Double?,
            locToLng: Double?
    ) : this(
            HistoryLocationSingle(locFromLat, locFromLng),
            HistoryLocationSingle(locToLat, locToLng))
}

class HistoryLocationSingle(
        var lat: Double? = null,
        var lng: Double? = null
)

