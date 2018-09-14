package com.team.eddie.uber_alles.utils

import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.QueryMap


interface SessionServices {

    /**
     * Login user
     *
     * @param username
     * @param password
     */
    @POST("login")
    fun login(@QueryMap params: Map<String, String>): Call<Map<String,Boolean>>

    @POST("register/check")
    fun registerCheck(@QueryMap params: Map<String, String>): Call<Boolean>

    @POST("register")
    fun register(@Query("userId") userId : String, @QueryMap params: @JvmSuppressWildcards Map<String, Any>): Call<Void>

}