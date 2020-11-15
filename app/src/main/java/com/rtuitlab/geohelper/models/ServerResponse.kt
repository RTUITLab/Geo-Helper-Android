package com.rtuitlab.geohelper.models

data class ServerResponse(
    val success: Boolean,
    val data: List<Place>
)