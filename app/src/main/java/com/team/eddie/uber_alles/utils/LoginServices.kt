package com.team.eddie.uber_alles.utils

import io.reactivex.Single
import retrofit2.Call
import retrofit2.http.Body
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
    @POST("login/previous")
    fun userLogin(@Field("username") username: String, @Field("password") password: String): Call<Void>

    @POST("login")
    fun login(@Body user: User): Single<User>

    /**
     * Logout
     */
    @POST("logout")
    fun logout(): Call<Void>
}