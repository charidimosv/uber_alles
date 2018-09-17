package com.team.eddie.uber_alles.ui.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.team.eddie.uber_alles.databinding.FragmentCustomerLogoutBinding
import com.team.eddie.uber_alles.ui.session.IntroActivity
import com.team.eddie.uber_alles.utils.SaveSharedPreference

class CustomerLogoutFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentCustomerLogoutBinding.inflate(inflater, container, false)

        SaveSharedPreference.cleanAll(activity!!.applicationContext)
        FirebaseAuth.getInstance().signOut()
        startActivity(IntroActivity.getLaunchIntent(activity!!))

        return binding.root
    }
}