package com.team.eddie.uber_alles.utils;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface LoginServices {

    /**
     * Login user
     *
     * @param username
     * @param password
     */
    @FormUrlEncoded
    @POST("login")
    Call<Void> userLogin(@Field("username") String username, @Field("password") String password);

    /**
     * Logout
     */
    @POST("logout")
    Call<Void> logout();
}