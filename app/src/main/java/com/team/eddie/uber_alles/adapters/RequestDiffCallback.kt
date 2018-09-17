package com.team.eddie.uber_alles.adapters

import androidx.recyclerview.widget.DiffUtil
import com.team.eddie.uber_alles.utils.firebase.Request

class RequestDiffCallback : DiffUtil.ItemCallback<Request>() {

    override fun areItemsTheSame(oldItem: Request, newItem: Request): Boolean {
        return oldItem.requestId == newItem.requestId
    }

    override fun areContentsTheSame(oldItem: Request, newItem: Request): Boolean {
        return oldItem == newItem
    }
}