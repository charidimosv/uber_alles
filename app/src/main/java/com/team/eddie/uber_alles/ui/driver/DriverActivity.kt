package com.team.eddie.uber_alles.ui.driver

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.databinding.ActivityDriverBinding
import com.team.eddie.uber_alles.ui.generic.UserActivity
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper

class DriverActivity : UserActivity() {

    companion object {
        fun getLaunchIntent(from: Context) = Intent(from, DriverActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        activityId = R.layout.activity_driver
        navFragmentId = R.id.driver_nav_fragment

        super.onCreate(savedInstanceState)

        val binding: ActivityDriverBinding = DataBindingUtil.setContentView(this, activityId)
        val navController = Navigation.findNavController(this, navFragmentId)

        drawerLayout = binding.drawerLayout

        // Toolbar :: Transparent
        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""

        NavigationUI.setupActionBarWithNavController(this, navController, drawerLayout)

        // Status bar :: Transparent
        // window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        binding.navigationView.setupWithNavController(navController)
        getSyncUserInfoDrawer()
    }

    override fun onStop() {
        FirebaseHelper.removeDriverAvailable(FirebaseHelper.getUserId())
        super.onStop()
    }
}