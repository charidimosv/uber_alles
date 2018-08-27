package com.team.eddie.uber_alles.ui.login

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.ui.welcome.WelcomeActivity
import com.team.eddie.uber_alles.utils.LoginServices
import com.team.eddie.uber_alles.utils.RetrofitClient
import com.team.eddie.uber_alles.utils.SaveSharedPreference
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivityOld : AppCompatActivity() {

    internal var username: EditText? = null
    internal var password: EditText? = null
    internal var submitBtn: Button? = null
    internal var loginForm: RelativeLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Check if UserResponse is Already Logged In
        if (SaveSharedPreference.isLoggedIn(applicationContext)) {
            val intent = Intent(applicationContext, WelcomeActivity::class.java)
            startActivity(intent)
        } else {
            loginForm!!.visibility = View.VISIBLE
        }


        submitBtn!!.setOnClickListener {
            // Make form visible

            userLogin(username!!.text.toString(), password!!.text.toString())
        }
    }

    /**
     * Login API call
     * TODO: Please modify according to your need it is just an example
     *
     * @param username
     * @param password
     */
    private fun userLogin(username: String, password: String) {

        val retrofit = RetrofitClient.getClient(this@LoginActivityOld)
        val loginServices = retrofit!!.create(LoginServices::class.java)
        val call = loginServices.userLogin(username, password)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {

                if (response.isSuccessful) {
                    // Set Logged In statue to 'true'
                    SaveSharedPreference.setLoggedIn(applicationContext, username)
                    val intent = Intent(applicationContext, WelcomeActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                } else {
                    Toast.makeText(applicationContext, "Credentials are not Valid.",
                            Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("TAG", "=======onFailure: " + t.toString())
                t.printStackTrace()
                // Log error here since request failed
            }
        })
    }
}
