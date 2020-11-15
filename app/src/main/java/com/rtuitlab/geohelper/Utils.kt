package com.rtuitlab.geohelper

import android.location.Location
import com.rtuitlab.geohelper.models.LatLng

fun Location.toLatLng() = LatLng(
    this.latitude,
    this.longitude
)