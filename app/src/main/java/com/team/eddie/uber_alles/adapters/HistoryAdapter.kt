package com.team.eddie.uber_alles.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.databinding.ItemHistoryBinding
import com.team.eddie.uber_alles.view.HistoryObject


class HistoryAdapter(
        private val itemList: List<HistoryObject>,
        private val context: Context)
    : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
                DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.item_history, parent, false
                )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val historyItem = itemList[position]

        holder.rideId.text = historyItem.rideId
        holder.time.text = historyItem.time
    }

    override fun getItemCount(): Int {
        return this.itemList.size
    }

    inner class ViewHolder(private val binding: ItemHistoryBinding)
        : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        var rideId: TextView
        var time: TextView

        init {
            itemView.setOnClickListener(this)

            rideId = itemView.findViewById<View>(R.id.rideId) as TextView
            time = itemView.findViewById<View>(R.id.time) as TextView
        }


        override fun onClick(v: View) {
            //        Intent intent = new Intent(v.getContext(), HistorySingleActivity.class);
            //        Bundle b = new Bundle();
            //        b.putString("rideId", rideId.getText().toString());
            //        intent.putExtras(b);
            //        v.getContext().startActivity(intent);
        }
    }
}