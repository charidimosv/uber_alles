package com.team.eddie.uber_alles.ui.driver

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.firebase.geofire.GeoFire
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.team.eddie.uber_alles.databinding.FragmentDriverLogoutBinding
import com.team.eddie.uber_alles.ui.session.WelcomeActivity
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import com.team.eddie.uber_alles.utils.SaveSharedPreference

class DriverLogoutFragment : Fragment() {

    private var isLoggingOut: Boolean = false;

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentDriverLogoutBinding.inflate(inflater, container, false)

        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        FirebaseHelper.getDriverCustomerRide(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        var customerId: String? = if (dataSnapshot.exists()) dataSnapshot.value!!.toString() else ""

                        if (TextUtils.isEmpty(customerId)) {
                            isLoggingOut = true
                            disconnectDriver()

                            SaveSharedPreference.cleanAll(activity!!.applicationContext)
                            FirebaseAuth.getInstance().signOut()
                            startActivity(WelcomeActivity.getLaunchIntent(activity!!))
                        } else
                            binding.driverLogout.setText("Ride must be ended before you can logout")
                    }

                    override fun onCancelled(databaseError: DatabaseError) {}
                })


        return binding.root
    }

    private fun disconnectDriver() {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val refAvailable = FirebaseHelper.getDriversAvailable()
        val refWorking = FirebaseHelper.getDriversWorking()
        val geoFireAvailable = GeoFire(refAvailable)
        val geoFireWorking = GeoFire(refWorking)

        geoFireAvailable.removeLocation(userId)
    }

    override fun onStop() {
        super.onStop()
        if (!isLoggingOut)
            disconnectDriver()
    }
}