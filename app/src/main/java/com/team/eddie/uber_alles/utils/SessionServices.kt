package com.team.eddie.uber_alles.utils

import com.team.eddie.uber_alles.utils.firebase.UserInfo
import retrofit2.Call
import retrofit2.http.Body
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
    fun login(@Body userInfo: UserInfo): Call<UserInfo>

    @POST("register/check")
    fun registerCheck(@QueryMap params: Map<String, String>): Call<Boolean>

    @POST("register")
    fun register(@Body userInfo: UserInfo): Call<Void>

}