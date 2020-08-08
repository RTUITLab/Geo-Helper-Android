package com.rtuitlab.geohelper.models

import android.location.Location

data class LocationData(
    val location: Location,
    val places: List<Place>
)