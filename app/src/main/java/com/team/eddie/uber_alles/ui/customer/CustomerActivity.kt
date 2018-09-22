package com.team.eddie.uber_alles.ui.customer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.databinding.ActivityCustomerBinding
import com.team.eddie.uber_alles.ui.generic.UserActivity

class CustomerActivity : UserActivity() {

    companion object {
        fun getLaunchIntent(from: Context) = Intent(from, CustomerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        activityId = R.layout.activity_customer
        navFragmentId = R.id.customer_nav_fragment

        super.onCreate(savedInstanceState)

        val binding: ActivityCustomerBinding = DataBindingUtil.setContentView(this, activityId)
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
}