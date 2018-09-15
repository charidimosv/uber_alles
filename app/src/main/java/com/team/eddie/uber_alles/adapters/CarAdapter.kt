package com.team.eddie.uber_alles.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.team.eddie.uber_alles.databinding.ItemCarBinding
import com.team.eddie.uber_alles.ui.driver.DriverCarListFragmentDirections
import com.team.eddie.uber_alles.utils.firebase.Car


class CarAdapter : ListAdapter<Car, CarAdapter.ViewHolder>(CarDiffCallback()) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemCarBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val car = getItem(position)
        holder.apply {
            bind(createOnClickListener(car.carId!!), car)
            itemView.tag = car
        }
    }

    private fun createOnClickListener(carId: String): View.OnClickListener {
        return View.OnClickListener {
            val direction = DriverCarListFragmentDirections.ActionDriverCarListFragmentToDriverCarSingleFragment(carId)
            it.findNavController().navigate(direction)
        }
    }

    inner class ViewHolder(
            private val binding: ItemCarBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: View.OnClickListener, item: Car) {
            binding.apply {
                clickListener = listener
                carItem = item
                executePendingBindings()
            }
        }
    }
}