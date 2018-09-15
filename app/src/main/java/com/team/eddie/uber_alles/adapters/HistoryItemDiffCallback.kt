package com.team.eddie.uber_alles.adapters

import androidx.recyclerview.widget.DiffUtil
import com.team.eddie.uber_alles.utils.firebase.HistoryItem

class HistoryItemDiffCallback : DiffUtil.ItemCallback<HistoryItem>() {

    override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
        return oldItem.rideId == newItem.rideId
    }

    override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
        return oldItem == newItem
    }
}