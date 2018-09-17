package com.team.eddie.uber_alles.ui.driver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.team.eddie.uber_alles.databinding.FragmentDriverLogoutBinding
import com.team.eddie.uber_alles.ui.session.IntroActivity
import com.team.eddie.uber_alles.utils.SaveSharedPreference
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper

class DriverLogoutFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentDriverLogoutBinding.inflate(inflater, container, false)

        disconnectDriver()

        SaveSharedPreference.cleanAll(activity!!.applicationContext)
        FirebaseAuth.getInstance().signOut()
        startActivity(IntroActivity.getLaunchIntent(activity!!))

        return binding.root
    }

    private fun disconnectDriver() {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid
        FirebaseHelper.removeDriverAvailable(userId)
    }
}