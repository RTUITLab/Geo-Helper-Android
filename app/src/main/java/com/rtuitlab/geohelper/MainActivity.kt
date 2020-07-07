package com.rtuitlab.geohelper

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {

	companion object {
		const val LOG_TAG = "GeoHelperLogs"
	}

	private val viewModel: MainViewModel by lazy {
		ViewModelProvider(this).get(MainViewModel::class.java)
	}

	private val azimuthManager by lazy {
		AzimuthManager(
				getSystemService(SensorManager::class.java)!!
		)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		setupLocationListener()

		viewModel.locationDataLiveData.observe(this, Observer {
			processLocationData(it)
		})
	}

	override fun onResume() {
		super.onResume()
		azimuthManager.startSensor()
	}

	override fun onPause() {
		super.onPause()
		azimuthManager.stopSensor()
	}

	private fun setupLocationListener() {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			Log.e(LOG_TAG, "PERMISSIONS NOT GRANTED")
			return
		}
		getSystemService(LocationManager::class.java)?.apply {
			requestLocationUpdates(LocationManager.GPS_PROVIDER, 200, 0F, object : ChangeLocationListener() {
				override fun onLocationChanged(location: Location?) {
					viewModel.azimuth = azimuthManager.azimuth
					viewModel.currentLocation = location
				}
			})
		}
	}

	private fun processLocationData(locationData: LocationData) {

	}
}