package com.team.eddie.uber_alles.ui.driver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.team.eddie.uber_alles.databinding.FragmentDriverHistoryBinding

class DriverHistoryFragment : androidx.fragment.app.Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentDriverHistoryBinding.inflate(inflater, container, false)
        context ?: return binding.root

        setHasOptionsMenu(true)
        return binding.root
    }
}