package com.team.eddie.uber_alles.adapters

import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.team.eddie.uber_alles.ui.ActivityHelper

@BindingAdapter("imageFromUrl")
fun bindImageFromUrl(view: ImageView, imageUrl: String?) {
    imageUrl?.let { if (it.isNotEmpty()) ActivityHelper.bindImageFromUrl(view, it) }
}

@BindingAdapter("dateFromLong")
fun bindDateFromLong(view: TextView, time: Long?) {
    time?.let { view.text = ActivityHelper.getDate(it) }
}

