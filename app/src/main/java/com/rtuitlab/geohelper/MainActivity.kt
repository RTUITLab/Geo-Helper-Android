package com.rtuitlab.geohelper

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.ar.core.Pose
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.view_place_label.view.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


class MainActivity : AppCompatActivity() {

	companion object {
		const val LOG_TAG = "GeoHelperLogs"
	}

	private val arFragment by lazy {
		supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment
	}

	private val scene: Scene by lazy {
		arFragment.arSceneView.scene
	}

	private val viewModel: MainViewModel by lazy {
		ViewModelProvider(this).get(MainViewModel::class.java)
	}

	private val azimuthManager by lazy {
		AzimuthManager(
				getSystemService(SensorManager::class.java)!!
		)
	}

	private var anchorNode: AnchorNode? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		setupLocationListener()

		viewModel.locationDataLiveData.observe(this, Observer {
			Log.i("asd", "asd")
//			processLocationData(it)
		})

		updateBtn.setOnClickListener {
			val locationData = viewModel.locationDataLiveData.value
			locationData?.let {
				processLocationData(it)
			} ?:run {
				Toast.makeText(this@MainActivity, "Not ready!", Toast.LENGTH_SHORT).show()
			}
		}
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
			requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0F, object : ChangeLocationListener() {
				override fun onLocationChanged(location: Location?) {
					viewModel.azimuth = azimuthManager.azimuth
					viewModel.currentLocation = location
					Toast.makeText(
						this@MainActivity,
						"Location changed",
						Toast.LENGTH_SHORT
					).show()
				}
			})
		}
	}

	private fun processLocationData(locationData: LocationData) {
		Log.wtf(LOG_TAG, locationData.location.toString())

		anchorNode?.anchor?.detach()
		anchorNode = null

		val anchor = arFragment.arSceneView.session?.createAnchor(
			Pose(
				floatArrayOf(0F, 0F, 0F),
				floatArrayOf(0F, 0F, 0F, 1F)
			)
		)
		anchorNode = AnchorNode(anchor).apply {
			setParent(scene)
		}

		locationData.places.forEach {
			val degree = it.bearingFrom(locationData.location)
			val distant = 3.0

			val y = 0F
			val x = (distant * cos(PI * degree / 180)).toFloat()
			val z = (-distant * sin(PI * degree / 180)).toFloat()
			addPointByXYZ(-z, y, -x, "${it.name}\nDegree: $degree\nX,Y,Z: $x, $y, $z")
		}
	}

	private fun addPointByXYZ(x: Float, y: Float, z: Float, name: String) {
		ViewRenderable
			.builder()
			.setView(this, R.layout.view_place_label)
			.build()
			.thenAccept { viewRenderable ->
				viewRenderable.view.labelTV.text = name

				anchorNode?.addChild(
					PlaceNode().apply {
						renderable = viewRenderable
						worldPosition = Vector3(x, y, z)
					}
				)

//				val node = PlaceNode()
//				node.renderable = it
//				scene.addChild(node)
//				node.worldPosition = Vector3(x, y, z)

//				val cameraPosition = scene.camera.worldPosition
//				val direction = Vector3.subtract(cameraPosition, node.worldPosition)
//				val lookRotation = Quaternion.lookRotation(direction, Vector3.up())
//				node.worldRotation = lookRotation

//				scene.addChild(node)
			}
	}
}