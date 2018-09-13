package com.team.eddie.uber_alles.ui.session

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.databinding.ActivityIntroBinding
import com.team.eddie.uber_alles.ui.customer.CustomerActivity
import com.team.eddie.uber_alles.ui.driver.DriverActivity
import com.team.eddie.uber_alles.utils.SaveSharedPreference

class IntroActivity : AppCompatActivity() {

    companion object {
        fun getLaunchIntent(from: Context) = Intent(from, IntroActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        // Check if user is already LoggedIn
        if (SaveSharedPreference.isLoggedIn(applicationContext)) {
            if (SaveSharedPreference.isDriver(applicationContext))
                startActivity(DriverActivity.getLaunchIntent(this))
            else
                startActivity(CustomerActivity.getLaunchIntent(this))
        }

        super.onCreate(savedInstanceState)
        val binding: ActivityIntroBinding = DataBindingUtil.setContentView(this, R.layout.activity_intro)
    }

}
