package com.rtuitlab.geohelper.models

data class Place (
    val name: String,
    val type: String,
    val description: String,
    val position: LatLng
)