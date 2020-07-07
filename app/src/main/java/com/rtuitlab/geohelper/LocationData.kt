package com.rtuitlab.geohelper

import android.location.Location

data class LocationData(
    val location: Location,
    val azimuth: Int,
    val places: List<Place>
)