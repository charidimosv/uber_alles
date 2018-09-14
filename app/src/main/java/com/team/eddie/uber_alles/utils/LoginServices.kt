package com.team.eddie.uber_alles.utils

import io.reactivex.Single
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.QueryMap


interface LoginServices {

    /**
     * Login user
     *
     * @param username
     * @param password
     */
    @POST("user/login")
    fun userLogin(@QueryMap params: Map<String, String>): Call<Void>

    @POST("login")
    fun login(@Body user: User): Single<User>

    /**
     * Logout
     */
    @POST("logout")
    fun logout(): Call<Void>
}