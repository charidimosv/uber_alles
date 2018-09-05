package com.team.eddie.uber_alles.ui.generic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.team.eddie.uber_alles.adapters.HistoryAdapter
import com.team.eddie.uber_alles.databinding.FragmentGenericHistoryBinding
import com.team.eddie.uber_alles.view.HistoryObject
import java.util.*

class GenericHistoryFragment : Fragment() {

    private lateinit var mAdapter: HistoryAdapter
    private lateinit var recyclerView: RecyclerView

    private var resultsHistoryList = ArrayList<HistoryObject>()


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentGenericHistoryBinding.inflate(inflater, container, false)
        context ?: return binding.root
        setHasOptionsMenu(true)

        recyclerView = binding.historyRecyclerView

        mAdapter = HistoryAdapter(resultsHistoryList, activity!!.applicationContext)
        val layoutManager = LinearLayoutManager(activity!!.applicationContext)

        recyclerView.layoutManager = layoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.adapter = mAdapter

//        recyclerView.isNestedScrollingEnabled = false
//        recyclerView.setHasFixedSize(true)

        prepareMovieData()

        return binding.root
    }

    private fun prepareMovieData() {
        resultsHistoryList.add(HistoryObject("1", "2"))
        resultsHistoryList.add(HistoryObject("3", "4"))
//        resultsHistoryList = HistoryObject.createHistoryList(5)

        mAdapter.notifyDataSetChanged()
    }
}