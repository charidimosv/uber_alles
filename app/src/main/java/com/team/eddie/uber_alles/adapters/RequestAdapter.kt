package com.team.eddie.uber_alles.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.team.eddie.uber_alles.databinding.ItemRequestBinding
import com.team.eddie.uber_alles.ui.generic.GenericRequestListFragmentDirections
import com.team.eddie.uber_alles.utils.firebase.Request


class RequestAdapter : ListAdapter<Request, RequestAdapter.ViewHolder>(RequestDiffCallback()) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = getItem(position)
        holder.apply {
            bind(createOnClickListener(request.requestId), request)
            itemView.tag = request
        }
    }

    private fun createOnClickListener(requestId: String): View.OnClickListener {
        return View.OnClickListener {
            val direction = GenericRequestListFragmentDirections.actionGenericHistoryFragmentToGenericHistorySingleFragment(requestId)
            it.findNavController().navigate(direction)
        }
    }

    inner class ViewHolder(
            private val binding: ItemRequestBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: View.OnClickListener, item: Request) {
            binding.apply {
                clickListener = listener
                requestItem = item
                executePendingBindings()
            }
        }
    }
}