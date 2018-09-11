package com.team.eddie.uber_alles.adapters

import androidx.recyclerview.widget.DiffUtil
import com.team.eddie.uber_alles.view.CarItem

class CarItemDiffCallback : DiffUtil.ItemCallback<CarItem>() {

    override fun areItemsTheSame(oldItem: CarItem, newItem: CarItem): Boolean {
        return oldItem.carId == newItem.carId
    }

    override fun areContentsTheSame(oldItem: CarItem, newItem: CarItem): Boolean {
        return oldItem == newItem
    }
}