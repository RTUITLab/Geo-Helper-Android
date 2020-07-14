package com.rtuitlab.geohelper

import android.location.Location
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class Place(
    val latitude: Double,
    val longitude: Double,
    val name: String
) {
    fun bearingFrom(location: Location): Double {
        val lat1 = location.latitude / 180 * PI
        val lng1 = location.longitude / 180 * PI
        val lat2 = this.latitude / 180 * PI
        val lng2 = this.longitude / 180 * PI

        val x = cos(lat2) * sin(lng2 - lng1)
        val y = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(lng2 - lng1)

        return ((atan2(x, y) * 180 / PI) + 360) % 360
    }
}