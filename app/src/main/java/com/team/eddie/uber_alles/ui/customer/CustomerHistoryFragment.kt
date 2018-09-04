package com.team.eddie.uber_alles.ui.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.team.eddie.uber_alles.databinding.FragmentCustomerHistoryBinding

class CustomerHistoryFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentCustomerHistoryBinding.inflate(inflater, container, false)
        context ?: return binding.root

        setHasOptionsMenu(true)
        return binding.root
    }
}