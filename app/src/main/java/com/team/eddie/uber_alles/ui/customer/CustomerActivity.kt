package com.team.eddie.uber_alles.ui.customer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.databinding.ActivityCustomerBinding

class CustomerActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout

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

        // Set up ActionBar
//        setSupportActionBar(binding.toolbar)
//        NavigationUI.setupActionBarWithNavController(this, navController, drawerLayout)

        // Set up navigation menu
        binding.navigationView.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(drawerLayout,
                Navigation.findNavController(this, R.id.customer_nav_fragment))
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START)
        else super.onBackPressed()
    }
}