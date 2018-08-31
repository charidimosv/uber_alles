package com.team.eddie.uber_alles.ui.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.firebase.geofire.GeoFire
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.team.eddie.uber_alles.databinding.FragmentCustomerLogoutBinding
import com.team.eddie.uber_alles.ui.session.WelcomeActivity
import com.team.eddie.uber_alles.utils.SaveSharedPreference

class CustomerLogoutFragment : androidx.fragment.app.Fragment() {

    private var isLoggingOut: Boolean = false;

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentCustomerLogoutBinding.inflate(inflater, container, false)

        if (!SaveSharedPreference.getActiveRequest(activity!!.applicationContext)) {
            isLoggingOut = true
            disconnectCustomer()
            SaveSharedPreference.cleanAll(activity!!.applicationContext)
            FirebaseAuth.getInstance().signOut()
            startActivity(WelcomeActivity.getLaunchIntent(activity!!))
        } else
            binding.customerLogout.setText("Ride must be ended before you can logout")

        return binding.root
    }

    private fun disconnectCustomer() {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val custRequest = FirebaseDatabase.getInstance().getReference("customerRequest")
        val geoFire = GeoFire(custRequest)
        geoFire.removeLocation(userId)
        //val refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable")
        //val refWorking = FirebaseDatabase.getInstance().getReference("driversWorking")
        //val geoFireAvailable = GeoFire(refAvailable)
        //val geoFireWorking = GeoFire(refWorking)

        //geoFireAvailable.removeLocation(userId)
    }

    override fun onStop() {
        super.onStop()
        if (!isLoggingOut)
            disconnectCustomer()
    }
}