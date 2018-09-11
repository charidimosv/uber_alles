package com.team.eddie.uber_alles.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.team.eddie.uber_alles.databinding.ItemHistoryBinding
import com.team.eddie.uber_alles.ui.generic.GenericHistoryListFragmentDirections
import com.team.eddie.uber_alles.view.HistoryItem


class HistoryAdapter : ListAdapter<HistoryItem, HistoryAdapter.ViewHolder>(HistoryItemDiffCallback()) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val historyItem = getItem(position)
        holder.apply {
            bind(createOnClickListener(historyItem.rideId), historyItem)
            itemView.tag = historyItem
        }
    }

    private fun createOnClickListener(rideId: String): View.OnClickListener {
        return View.OnClickListener {
            val direction = GenericHistoryListFragmentDirections.ActionGenericHistoryFragmentToGenericHistorySingleFragment(rideId)
            it.findNavController().navigate(direction)
        }
    }

    inner class ViewHolder(
            private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: View.OnClickListener, item: HistoryItem) {
            binding.apply {
                clickListener = listener
                historyItem = item
                executePendingBindings()
            }
        }
    }
}