package com.rtuitlab.geohelper.azimuth

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener

abstract class ChangeSensorListener: SensorEventListener {
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    abstract override fun onSensorChanged(event: SensorEvent)
}