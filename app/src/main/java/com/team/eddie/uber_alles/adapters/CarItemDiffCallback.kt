package com.team.eddie.uber_alles.adapters

import androidx.recyclerview.widget.DiffUtil
import com.team.eddie.uber_alles.utils.firebase.Car

class CarDiffCallback : DiffUtil.ItemCallback<Car>() {

    override fun areItemsTheSame(oldItem: Car, newItem: Car): Boolean {
        return oldItem.carId == newItem.carId
    }

    override fun areContentsTheSame(oldItem: Car, newItem: Car): Boolean {
        return oldItem == newItem
    }
}