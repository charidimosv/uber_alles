package com.team.eddie.uber_alles.ui.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.firebase.geofire.GeoFire
import com.google.firebase.auth.FirebaseAuth
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.databinding.FragmentCustomerLogoutBinding
import com.team.eddie.uber_alles.ui.session.WelcomeActivity
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import com.team.eddie.uber_alles.utils.SaveSharedPreference

class CustomerLogoutFragment : Fragment() {

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
            binding.customerLogout.text = getString(R.string.end_ride_before_logout)

        return binding.root
    }

    private fun disconnectCustomer() {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        val custRequest = FirebaseHelper.getCustomerRequest()
        val geoFire = GeoFire(custRequest)
        geoFire.removeLocation(userId)
    }

    override fun onStop() {
        super.onStop()
        if (!isLoggingOut) disconnectCustomer()
    }
}