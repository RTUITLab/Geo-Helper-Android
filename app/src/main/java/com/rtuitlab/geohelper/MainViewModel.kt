package com.rtuitlab.geohelper

import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.neovisionaries.ws.client.*
import com.rtuitlab.geohelper.models.LatLng
import com.rtuitlab.geohelper.models.Place
import com.rtuitlab.geohelper.models.ServerResponse

class MainViewModel: ViewModel() {

    var currentLocation: Location? = null
        set(value) {
            field = value
            checkNewLocationData()
        }

    private val webSocket = WebSocketFactory()
        .createSocket("wss://geo-helper.herokuapp.com/").apply {
            addListener(object : WebSocketAdapter() {
                override fun onTextMessage(websocket: WebSocket?, text: String?) {
                    Log.wtf("hey", "RESPONSE: $text")
                    if (!text.isNullOrBlank() && !text.contains("message")) {
                        val response = Gson().fromJson(text, ServerResponse::class.java)
                        if (response.success) {
                            Log.wtf("hey", "SUCCESS RESPONSE: $response")
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
        Log.wtf("hey", "checkNewLocationData")
        currentLocation?.let {
            if (webSocket.isOpen) {
                Log.wtf("hey", "sendText")
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