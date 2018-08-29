package com.team.eddie.uber_alles.ui.welcome

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.ui.login.LoginActivity
import com.team.eddie.uber_alles.ui.map.CustomerMapActivity
import com.team.eddie.uber_alles.ui.map.DriverMapActivity
import com.team.eddie.uber_alles.ui.register.RegisterActivity
import com.team.eddie.uber_alles.utils.SaveSharedPreference
import com.team.eddie.uber_alles.utils.onClick
import kotlinx.android.synthetic.main.activity_welcome.*

class WelcomeActivity : AppCompatActivity() {

    companion object {
        fun getLaunchIntent(from: Context) = Intent(from, WelcomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Check if user is already LoggedIn
        if (SaveSharedPreference.isLoggedIn(applicationContext)) startMainScreen()

        registerButton.onClick { startActivity(Intent(this, RegisterActivity::class.java)) }
        loginButton.onClick { startActivity(Intent(this, LoginActivity::class.java)) }
    }

    fun startMainScreen() {
        if (SaveSharedPreference.isDriver(applicationContext))
            startActivity(DriverMapActivity.getLaunchIntent(this))
        else
            startActivity(CustomerMapActivity.getLaunchIntent(this))
    }
}
