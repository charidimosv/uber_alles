package com.team.eddie.uber_alles.ui.customer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation
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
        super.onCreate(savedInstanceState)

        val binding: ActivityCustomerBinding = DataBindingUtil.setContentView(this, R.layout.activity_customer)
        drawerLayout = binding.drawerLayout

        val navController = Navigation.findNavController(this, R.id.customer_nav_fragment)
        binding.navigationView.setupWithNavController(navController)

        getSyncUserInfoDrawer()
    }
}