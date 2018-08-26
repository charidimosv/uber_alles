package com.team.eddie.uber_alles.utils

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface LoginServices {

    /**
     * Login user
     *
     * @param username
     * @param password
     */
    @FormUrlEncoded
    @POST("login")
    fun userLogin(@Field("username") username: String, @Field("password") password: String): Call<Void>

    /**
     * Logout
     */
    @POST("logout")
    fun logout(): Call<Void>
}