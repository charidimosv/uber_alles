package com.team.eddie.uber_alles.ui.driver

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation
import androidx.navigation.ui.setupWithNavController
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.databinding.ActivityDriverBinding
import com.team.eddie.uber_alles.ui.generic.UserActivity

class DriverActivity : UserActivity() {

    companion object {
        fun getLaunchIntent(from: Context) = Intent(from, DriverActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityDriverBinding = DataBindingUtil.setContentView(this, R.layout.activity_driver)
        drawerLayout = binding.drawerLayout

        val navController = Navigation.findNavController(this, R.id.driver_nav_fragment)
        binding.navigationView.setupWithNavController(navController)

        getSyncUserInfoDrawer()
    }
}