package com.team.eddie.uber_alles.ui

import android.text.format.DateFormat
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import java.util.*


object ActivityHelper {

    // Glide Binding

    fun bindImageFromUrl(view: ImageView, imageUrl: Any) {
        bindImageFromUrl(view, imageUrl.toString())
    }

    fun bindImageFromUrlSimple(view: ImageView, imageUrl: String) {
        Glide.with(view.context)
                .load(imageUrl)
                .into(view)
    }

    fun bindImageFromUrl(view: ImageView, imageUrl: String) {
        Glide.with(view.context)
                .load(imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(view)
    }

    fun getDate(time: Long?): String {
        val cal = Calendar.getInstance(Locale.getDefault())
        cal.timeInMillis = time!! * 1000
        return DateFormat.format("MM-dd-yyyy hh:mm", cal).toString()
    }

}