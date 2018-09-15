package com.team.eddie.uber_alles.utils.firebase

class Request(
        var requestId: String = "",

        var customer: String = "",
        var driver: String = "",

        var pickupTime: Long? = null
)