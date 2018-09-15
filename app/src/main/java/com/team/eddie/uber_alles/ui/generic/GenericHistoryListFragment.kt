package com.team.eddie.uber_alles.ui.generic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.team.eddie.uber_alles.adapters.HistoryAdapter
import com.team.eddie.uber_alles.databinding.FragmentGenericHistoryListBinding
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import com.team.eddie.uber_alles.utils.firebase.HistoryItem
import java.util.*

class GenericHistoryListFragment : Fragment() {

    private lateinit var binding: FragmentGenericHistoryListBinding

    private lateinit var mAdapter: HistoryAdapter
    private lateinit var recyclerView: RecyclerView

    private lateinit var userId: String
    private var resultsHistoryList = ArrayList<HistoryItem>()

    private lateinit var mBalance: TextView
    private var balance: Double = 0.0

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGenericHistoryListBinding.inflate(inflater, container, false)
        context ?: return binding.root
        setHasOptionsMenu(true)

        mAdapter = HistoryAdapter()
        mAdapter.submitList(resultsHistoryList)

        mBalance = binding.balance
        recyclerView = binding.historyRecyclerView
        recyclerView.adapter = mAdapter

        userId = FirebaseHelper.getUserId()
        getUserHistoryIds()

        return binding.root
    }

    private fun getUserHistoryIds() {
        val userHistoryDatabase = FirebaseHelper.getUserHistory(userId)
        userHistoryDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists())
                    for (history in dataSnapshot.children) fetchRideInformation(history.key)

                if (resultsHistoryList.isEmpty()) {
                    binding.layout.visibility = View.GONE
                    binding.noHistory.visibility = View.VISIBLE
                } else {
                    binding.layout.visibility = View.VISIBLE
                    binding.noHistory.visibility = View.GONE
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun fetchRideInformation(rideKey: String?) {
        val historyDatabase = FirebaseHelper.getHistoryKey(rideKey!!)
        historyDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val historyItem = dataSnapshot.getValue(HistoryItem::class.java)
                    historyItem?.let {
                        it.rideId = dataSnapshot.key

                        resultsHistoryList.add(it)
                        mAdapter.notifyDataSetChanged()
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }
}