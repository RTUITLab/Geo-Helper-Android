package com.rtuitlab.geohelper

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.neovisionaries.ws.client.*
import com.rtuitlab.geohelper.models.LatLng
import com.rtuitlab.geohelper.models.Place
import com.rtuitlab.geohelper.models.ServerResponse

class MainViewModel: ViewModel() {

    companion object {
        const val API_URL = "wss://geo-helper.ga/api/v1/"
    }

    var currentLocation: Location? = null
        set(value) {
            field = value
            checkNewLocationData()
        }

    private val webSocket = WebSocketFactory()
        .createSocket(API_URL).apply {
            addListener(object : WebSocketAdapter() {
                override fun onTextMessage(websocket: WebSocket?, text: String?) {
                    text?.takeIf {
                        !it.contains("message")
                    }?.let { checkedText ->
                        Gson().fromJson(checkedText, ServerResponse::class.java).takeIf { response ->
                            response.success && response.data != locationDataLiveData.value
                        }?.let { response ->
                            _locationDataLiveData.postValue(response.data)
                        }
                    }
                }
            })
            connectAsynchronously()
        }

    private val _locationDataLiveData = MutableLiveData<List<Place>>()
    val locationDataLiveData: LiveData<List<Place>> = _locationDataLiveData

    private fun checkNewLocationData() {
        currentLocation?.let { location ->
            webSocket.takeIf {
                it.isOpen
            }?.sendText(
                Gson().toJson(
                    LatLng(location.latitude, location.longitude)
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocket.disconnect()
    }
}