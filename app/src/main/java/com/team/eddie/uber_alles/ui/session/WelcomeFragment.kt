package com.team.eddie.uber_alles.ui.session

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.team.eddie.uber_alles.databinding.FragmentWelcomeBinding

class WelcomeFragment : Fragment() {

    private lateinit var applicationContext: Context

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentWelcomeBinding.inflate(inflater, container, false)

        binding.loginButton.setOnClickListener {
            val direction = WelcomeFragmentDirections.actionWelcomeFragmentToLoginFragment()
            it.findNavController().navigate(direction)
        }
        binding.registerButton.setOnClickListener {
            val direction = WelcomeFragmentDirections.actionWelcomeFragmentToRegisterFragment()
            it.findNavController().navigate(direction)
        }

        return binding.root
    }

}