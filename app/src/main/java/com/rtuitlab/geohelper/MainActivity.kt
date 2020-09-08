package com.rtuitlab.geohelper

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.rtuitlab.geohelper.models.LocationData
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.view_place_label.view.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


class MainActivity : AppCompatActivity(), Scene.OnUpdateListener {

	companion object {
		const val LOG_TAG = "GeoHelperLogs"
	}

	private val arFragment by lazy {
		supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment
	}

	private val frame: Frame? by lazy {
		arFragment.arSceneView.arFrame
	}

	private val scene: Scene by lazy {
		arFragment.arSceneView.scene
	}

	private val viewModel: MainViewModel by lazy {
		ViewModelProvider(this).get(MainViewModel::class.java)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		setupLocationListener()
		arFragment.arSceneView.scene.addOnUpdateListener(this)

		viewModel.locationDataLiveData.observe(this, Observer {
			processLocationData(it)
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

//	override fun onResume() {
//		super.onResume()
//		azimuthManager.startSensor()
//	}
//
//	override fun onPause() {
//		super.onPause()
//		azimuthManager.stopSensor()
//	}

	private fun setupLocationListener() {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			Log.e(LOG_TAG, "PERMISSIONS NOT GRANTED")
			return
		}
		getSystemService(LocationManager::class.java)?.apply {
			requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1F, object : ChangeLocationListener() {
				override fun onLocationChanged(location: Location?) {
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

		locationData.places.forEach {
			val degree = it.bearingFrom(locationData.location.toLatLng())
			val distant = 3.0

			val y = 0F
			val x = (distant * cos(PI * degree / 180)).toFloat()
			val z = (-distant * sin(PI * degree / 180)).toFloat()
			addPointByXYZ(-z, y, -x, "${it.name}\nDegree: $degree\nX,Y,Z: $x, $y, $z")
//			addPointByXYZ(x, y, z, "${it.name}\nDegree: $degree\nX,Y,Z: $x, $y, $z")
		}
	}

	private fun addPointByXYZ(x: Float, y: Float, z: Float, name: String) {
		ViewRenderable.builder().setView(this, R.layout.view_place_label).build().thenAccept {
			it.view.labelTV.text = name

			val node = PlaceNode().apply {
				renderable = it
			}
			arFragment.arSceneView.scene.addChild(node)
			node.worldPosition = Vector3(x, y, z)
		}
	}

	override fun onUpdate(frameTime: FrameTime?) {
		frame?.let {
			for (o in it.getUpdatedTrackables(Plane::class.java)) {
				val plane = o as Plane
				if (plane.trackingState === TrackingState.TRACKING) {
					arFragment.planeDiscoveryController.hide()
					val iterableAnchor = it.updatedAnchors.iterator()
					if (!iterableAnchor.hasNext()) {
						makeAr(plane, it)
					}
				}
			}
		}
	}

	fun makeAr(plane: Plane, frame: Frame) {
		for (k in 0..9) {
			if (this.degree >= 160 && this.degree <= 170) {
				Toast.makeText(this, "walk", Toast.LENGTH_SHORT).show()
				val hitTest: List<HitResult> = frame.hitTest(screenCenter().x, screenCenter().y)
				val hitTestIterator = hitTest.iterator()
				while (hitTestIterator.hasNext()) {
					val hitResult: HitResult = hitTestIterator.next()
					modelAnchor = null
					modelAnchor = plane.createAnchor(hitResult.hitPose)
					val anchorNode =
						AnchorNode(modelAnchor)
					anchorNode.setParent(scene)
					val transformableNode =
						TransformableNode(arFragment.transformationSystem)
					transformableNode.setParent(anchorNode)
					transformableNode.renderable = this@MainActivity.andyRenderable
					val x: Float = modelAnchor.getPose().tx()
					val y: Float = modelAnchor.getPose().compose(Pose.makeTranslation(0f, 0f, 0f)).ty()
					transformableNode.worldPosition = Vector3(x, y, (-k).toFloat())
				}
			}
		}
	}

	private fun screenCenter(): Vector3 {
		val vw: View = findViewById(android.R.id.content)
		return Vector3(vw.width / 2f, vw.height / 2f, 0f)
	}

//	private fun addPointByXYZ(x: Float, y: Float, z: Float, name: String) {
//		ViewRenderable
//			.builder()
//			.setView(this, R.layout.view_place_label)
//			.build()
//			.thenAccept { viewRenderable ->
//				viewRenderable.view.labelTV.text = name
//
//				anchorNode?.addChild(
//					PlaceNode().apply {
//						renderable = viewRenderable
//						worldPosition = Vector3(x, y, z)
//					}
//				)
//
////				val node = PlaceNode()
////				node.renderable = it
////				scene.addChild(node)
////				node.worldPosition = Vector3(x, y, z)
//
////				val cameraPosition = scene.camera.worldPosition
////				val direction = Vector3.subtract(cameraPosition, node.worldPosition)
////				val lookRotation = Quaternion.lookRotation(direction, Vector3.up())
////				node.worldRotation = lookRotation
//
////				scene.addChild(node)
//			}
//	}
}