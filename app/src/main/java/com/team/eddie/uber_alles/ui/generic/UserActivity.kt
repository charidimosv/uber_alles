package com.team.eddie.uber_alles.ui.generic

import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.ui.ActivityHelper
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import com.team.eddie.uber_alles.utils.firebase.UserInfo

open class UserActivity : AppCompatActivity() {

    protected var activityId: Int = 0
    protected var navFragmentId: Int = 0
    protected lateinit var drawerLayout: DrawerLayout

    protected fun getSyncUserInfoDrawer() {
        val userID = FirebaseHelper.getUserId()
        val userInfoRef = FirebaseHelper.getUserInfo(userID)

        userInfoRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                    val userInfo = dataSnapshot.getValue(UserInfo::class.java)
                    userInfo ?: return

                    userInfo.imageUrl?.let {
                        val profImage = findViewById<ImageView>(R.id.imageViewDrawer)
                        ActivityHelper.bindImageFromUrl(profImage, it)
                    }
                    userInfo.name.let {
                        val name = findViewById<TextView>(R.id.nameDrawer)
                        name.text = it.toString()
                    }
                    userInfo.email.let {
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
                Navigation.findNavController(this, navFragmentId))
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START)
        else super.onBackPressed()
    }
}