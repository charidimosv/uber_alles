package com.team.eddie.uber_alles.utils.firebase

class Car(
        var carId: String? = null,

        var brand: String? = null,
        var model: String? = null,
        var color: String? = null,
        var plate: String? = null,
        var year: String? = null,
        var defaultCar: String = "false",

        var imageUrl: String? = null

) {
    fun defaultCarToBoolean(): Boolean {
        return defaultCar.toBoolean()
    }
}