package com.team.eddie.uber_alles.utils

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import okhttp3.JavaNetCookieJar
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.util.*
import java.util.concurrent.TimeUnit

object RetrofitClient {

    //TODO: ENTER YOUR API BASE URL
    val BASE_URL = "http://ENTER YOUR API BASE URL"  // Production

    var retrofit: Retrofit? = null
        private set

    // set your desired log level
    val client: Retrofit?
        get() {

            if (checkClient()) {
                return retrofit
            }

            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY

            val cookieHandler = CookieManager()
            val client = okhttp3.OkHttpClient.Builder().addNetworkInterceptor(interceptor)
                    .cookieJar(JavaNetCookieJar(cookieHandler))
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()

            val gson = GsonBuilder()
                    .setLenient()
                    .registerTypeAdapter(Date::class.java, JsonDeserializer { jsonElement, type, context -> Date(jsonElement.asJsonPrimitive.asLong) })
                    .create()

            retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(client)
                    .build()
            return retrofit
        }

    private fun checkClient(): Boolean {
        return retrofit != null
    }
}
