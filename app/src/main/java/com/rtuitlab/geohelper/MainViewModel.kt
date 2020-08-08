package com.rtuitlab.geohelper

import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketFactory
import com.rtuitlab.geohelper.models.LatLng
import com.rtuitlab.geohelper.models.LocationData
import com.rtuitlab.geohelper.models.ServerResponse

class MainViewModel: ViewModel() {

    companion object {
        private const val SERVER_URL = "wss://geo-helper.herokuapp.com/"
    }

    var currentLocation: Location? = null
        set(value) {
            field = value
            checkNewLocationData()
        }

    private val webSocket = WebSocketFactory()
        .createSocket(SERVER_URL).apply {
            addListener(object : WebSocketAdapter() {
                override fun onTextMessage(websocket: WebSocket?, text: String?) {
                    Log.wtf("hey", "RESPONSE: $text")
                    if (!text.isNullOrBlank() && !text.contains("message")) {
                        val response = Gson().fromJson(text, ServerResponse::class.java)
                        if (response.success) {
                            _locationDataLiveData.postValue(
                                LocationData(
                                    currentLocation!!,
                                    response.data
                                )
                            )
                        }
                    }
                }
            })
            connectAsynchronously()
        }

    private val _locationDataLiveData = MutableLiveData<LocationData>()
    val locationDataLiveData: LiveData<LocationData> = _locationDataLiveData

    private fun checkNewLocationData() {
        currentLocation?.let {
            if (webSocket.isOpen) {
                webSocket.sendText(
                    Gson().toJson(
                        LatLng(it.latitude, it.longitude)
                    )
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocket.disconnect()
    }
}