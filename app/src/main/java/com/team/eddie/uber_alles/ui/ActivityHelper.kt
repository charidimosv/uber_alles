package com.team.eddie.uber_alles.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.text.format.DateFormat
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.directions.route.AbstractRouting
import com.directions.route.Routing
import com.directions.route.RoutingListener
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.team.eddie.uber_alles.R
import java.util.*


object ActivityHelper {


    /*
    ----------------------------------
    GLIDE
    ----------------------------------
    */

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

    /*
    ----------------------------------
    ROUTING
    ----------------------------------
    */

    fun getRouteToMarker(wayPoints: List<LatLng>, listener: RoutingListener) {
        val routing = Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(listener)
                .alternativeRoutes(false)
                .waypoints(wayPoints)
                .build()
        routing.execute()
    }

    /*
    ----------------------------------
    BITMAP ZOOMED
    ----------------------------------
    */

    fun getPinBitmap(applicationContext: Context): BitmapDescriptor? {
        val bitmapDrawable = ContextCompat.getDrawable(applicationContext, R.drawable.ic_place) as BitmapDrawable
        val scaledDrawable = Bitmap.createScaledBitmap(bitmapDrawable.bitmap, 120, 120, false)
        return BitmapDescriptorFactory.fromBitmap(scaledDrawable)
    }

}