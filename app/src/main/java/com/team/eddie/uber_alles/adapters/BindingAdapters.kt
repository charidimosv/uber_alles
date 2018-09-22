package com.team.eddie.uber_alles.adapters

import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.team.eddie.uber_alles.ui.ActivityHelper
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import com.team.eddie.uber_alles.utils.firebase.Request

@BindingAdapter("imageFromUrl")
fun bindImageFromUrl(view: ImageView, imageUrl: String?) {
    imageUrl?.let { if (it.isNotEmpty()) ActivityHelper.bindImageFromUrl(view, it) }
}

@BindingAdapter("dateFromLong")
fun bindDateFromLong(view: TextView, time: Long?) {
    time?.let { view.text = ActivityHelper.getDate(it) }
}


@BindingAdapter("destinationFromRequest")
fun bindDateFromLong(view: TextView, requestId: String) {
    val requestRef = FirebaseHelper.getRequestKey(requestId)
    val requestListener = requestRef.addValueEventListener(object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                val currentRequest = dataSnapshot.getValue(Request::class.java)
                currentRequest ?: return
                var destinationAll: String = ""
                for (loc in currentRequest.destinationList!!)
                    destinationAll = loc.locName + " "
                view.text = destinationAll
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {}
    })
}

