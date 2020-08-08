package com.rtuitlab.geohelper.models

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class Place (
    val name: String,
    val type: String,
    val description: String,
    val position: LatLng
) {
    fun bearingFrom(currentLocation: LatLng): Double {
        val lat1 = currentLocation.lat / 180 * PI
        val lng1 = currentLocation.lng / 180 * PI
        val lat2 = this.position.lat / 180 * PI
        val lng2 = this.position.lng / 180 * PI

        val x = cos(lat2) * sin(lng2 - lng1)
        val y = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(lng2 - lng1)

        return ((atan2(x, y) * 180 / PI) + 360) % 360
    }
}