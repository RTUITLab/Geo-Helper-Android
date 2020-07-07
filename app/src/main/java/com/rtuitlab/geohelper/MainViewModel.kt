package com.rtuitlab.geohelper

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {

    var currentLocation: Location? = null
        set(value) {
            field = value
            checkNewLocationData()
        }

    var azimuth: Int? = null

    private val placesList: List<Place> = listOf(
        Place(55.670002, 37.480212, "РТУ МИРЭА"),
        Place(56.126895, 40.397134, "Золотые ворота"),
        Place(59.939817, 30.314448, "Эрмитаж")
    )

    private val _locationDataLiveData = MutableLiveData<LocationData>()
    val locationDataLiveData: LiveData<LocationData> = _locationDataLiveData

    private fun checkNewLocationData() {
        if (currentLocation != null && azimuth != null) {
            _locationDataLiveData.value = LocationData(
                currentLocation!!,
                azimuth!!,
                placesList
            )
        }
    }
}