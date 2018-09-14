package com.team.eddie.uber_alles.ui.generic

import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper

open class UserActivity : AppCompatActivity() {

    protected lateinit var drawerLayout: DrawerLayout

    protected fun getSyncUserInfoDrawer() {
        val userID = FirebaseHelper.getUserId()
        val userInfoRef = FirebaseHelper.getUserInfo(userID)

        userInfoRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                    val map = dataSnapshot.value as Map<String, Any>

                    map[FirebaseHelper.PROFILE_IMG_URL]?.let {
                        val profImage = findViewById<ImageView>(R.id.imageViewDrawer)
                        Glide.with(application).load(it.toString()).apply(RequestOptions.circleCropTransform()).into(profImage)
                    }
                    map[FirebaseHelper.NAME]?.let {
                        val name = findViewById<TextView>(R.id.nameDrawer)
                        name.text = it.toString()
                    }
                    map[FirebaseHelper.USERNAME]?.let {
                        val email = findViewById<TextView>(R.id.emailDrawer)
                        email.text = it.toString()
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(drawerLayout,
                Navigation.findNavController(this, R.id.driver_nav_fragment))
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START)
        else super.onBackPressed()
    }
}