package com.rtuitlab.geohelper.azimuth

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AzimuthManager(
    private val sensorManager: SensorManager
) {
    private val hasRotationVectorSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null

    private var rMat = FloatArray(9)
    private var orientation = FloatArray(3)
    private val lastAccelerometer = FloatArray(3)
    private val lastMagnetometer = FloatArray(3)

    var azimuth: Int? = null
        private set

    private val sensorListener = object : ChangeSensorListener() {
        override fun onSensorChanged(event: SensorEvent) {
            GlobalScope.launch(Dispatchers.IO) {
                if (hasRotationVectorSensor) {
                    SensorManager.getRotationMatrixFromVector(rMat, event.values)
                    azimuth = (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0].toDouble()) + 360).toInt() % 360
                } else {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.size)
                    } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                        System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.size)
                    }

                    if (lastAccelerometer.sum() + lastMagnetometer.sum() != 0F) {
                        SensorManager.getRotationMatrix(rMat, null, lastAccelerometer, lastMagnetometer)
                        SensorManager.getOrientation(rMat, orientation)
                        azimuth = (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0].toDouble()) + 360).toInt() % 360
                    }
                }
            }
        }
    }

    fun startSensor() {
        if (hasRotationVectorSensor) {
            sensorManager.registerListener(
                sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_UI
            )
        } else {
            sensorManager.registerListener(
                sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI
            )
            sensorManager.registerListener(
                sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    fun stopSensor() {
        sensorManager.unregisterListener(sensorListener)
    }
}