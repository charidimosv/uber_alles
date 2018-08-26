package com.team.eddie.uber_alles.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.utils.LoginServices
import com.team.eddie.uber_alles.utils.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class WelcomeActivity : AppCompatActivity() {

    internal var logoutBT: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        logoutBT = findViewById(R.id.logoutBT)
    }

    /**
     * Logout
     * TODO: Please modify according to your need it is just an example
     */
    fun logout() {
        val retrofit = RetrofitClient.client
        val loginServices = retrofit!!.create(LoginServices::class.java)
        val logout = loginServices.logout()

        logout.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.code() == 200) {
                    val intent = Intent(applicationContext, LoginActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("TAG", "=======onFailure: " + t.toString())
                t.printStackTrace()
            }
        })
    }
}
