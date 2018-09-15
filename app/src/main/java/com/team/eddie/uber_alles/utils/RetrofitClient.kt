package com.team.eddie.uber_alles.utils

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.CookieManager
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

interface RetrofitClient {

    companion object Factory {

        //TODO: ENTER YOUR API BASE URL
        val BASE_URL = "https://SNF-838608.vm.okeanos.grnet.gr:8443/"  // Production

        var retrofit: Retrofit? = null
            private set

        // set your desired log level
        //val client: Retrofit?
        //    get() {
        fun getClient(context: Context): Retrofit? {

            if (checkClient()) {
                return retrofit
            }

            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY

            val cookieHandler = CookieManager()
            val clientBuilder = sslConnection(context)
            val client = clientBuilder!!.addNetworkInterceptor(interceptor)
                    .cookieJar(JavaNetCookieJar(cookieHandler))
                    .connectTimeout(1000, TimeUnit.SECONDS)
                    .writeTimeout(1000, TimeUnit.SECONDS)
                    .readTimeout(3000, TimeUnit.SECONDS)
                    .build()

            val gson = GsonBuilder()
                    .setLenient()
                    .registerTypeAdapter(Date::class.java, JsonDeserializer { jsonElement, _, context -> Date(jsonElement.asJsonPrimitive.asLong) })
                    .create()



            retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .client(client)
                    .build()
            return retrofit
        }

        private fun checkClient(): Boolean {
            return retrofit != null
        }

        private fun sslConnection(context: Context): OkHttpClient.Builder? {

            // Load CAs from an InputStream
            // (could be from a resource or ByteArrayInputStream or ...)
            val cf: CertificateFactory = CertificateFactory.getInstance("X.509")

            // From https://www.washington.edu/itconnect/security/ca/load-der.crt
            val caInput: InputStream = BufferedInputStream(context.assets.open("localhost.crt"))
            val cidInput: InputStream = BufferedInputStream(context.assets.open("cid.p12"))

            val ca: X509Certificate = caInput.use {
                cf.generateCertificate(it) as X509Certificate
            }


            // Create a KeyStore containing our trusted CAs
            val keyStore = KeyStore.getInstance("PKCS12").apply {
                load(cidInput, "eddie123".toCharArray())
                setCertificateEntry("localhost", ca)
            }

            // Create a TrustManager that trusts the CAs inputStream our KeyStore
            val kmfAlgorithm: String = KeyManagerFactory.getDefaultAlgorithm()
            val kmf: KeyManagerFactory = KeyManagerFactory.getInstance(kmfAlgorithm).apply {
                init(keyStore, "eddie123".toCharArray())
            }
            // Create a TrustManager that trusts the CAs inputStream our KeyStore
            val tmfAlgorithm: String = TrustManagerFactory.getDefaultAlgorithm()
            val tmf: TrustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm).apply {
                init(keyStore)
            }

            // Create an SSLContext that uses our TrustManager
            val context: SSLContext = SSLContext.getInstance("TLS").apply {
                init(kmf.keyManagers, tmf.trustManagers, null)
            }

            return OkHttpClient.Builder().sslSocketFactory(context.socketFactory, tmf.trustManagers.get(0) as X509TrustManager)

        }
    }
}
