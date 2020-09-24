package com.rtuitlab.geohelper

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.ViewRenderable
import com.rtuitlab.geohelper.AugmentedRealityLocationUtils.INITIAL_MARKER_SCALE_MODIFIER
import com.rtuitlab.geohelper.AugmentedRealityLocationUtils.INVALID_MARKER_SCALE_MODIFIER
import com.rtuitlab.geohelper.models.Place
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.view_place_label.view.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.co.appoly.arcorelocation.LocationMarker
import uk.co.appoly.arcorelocation.LocationScene
import uk.co.appoly.arcorelocation.sensor.DeviceLocationChanged
import java.util.concurrent.CompletableFuture


class MainActivity : AppCompatActivity() {

	companion object {
		const val LOG_TAG = "GeoHelperLogs"
	}

	private val viewModel: MainViewModel by lazy {
		ViewModelProvider(this).get(MainViewModel::class.java)
	}

	private var locationScene: LocationScene? = null

	private val arHandler = Handler(Looper.getMainLooper())

	private var areAllMarkersLoaded = false

	private var arCoreInstallRequested = false

	private val resumeArElementsTask = Runnable {
		locationScene?.resume()
		arSceneView.resume()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		viewModel.locationDataLiveData.observe(this, Observer {
			processPlaces(it)
		})
	}

	override fun onResume() {
		super.onResume()
		setupSession()
	}

	override fun onPause() {
		super.onPause()
		pauseARElements()
	}

	private fun pauseARElements() {
		arSceneView.session?.let {
			locationScene?.pause()
			arSceneView?.pause()
		}
	}

	private fun setupSession() {
		if (arSceneView == null) {
			return
		}

		if (arSceneView.session == null) {
			try {
				val session = AugmentedRealityLocationUtils.setupSession(this, arCoreInstallRequested)
				if (session == null) {
					arCoreInstallRequested = true
					return
				} else {
					arSceneView.setupSession(session)
				}
			} catch (e: UnavailableException) {
				AugmentedRealityLocationUtils.handleSessionException(this, e)
			}
		}

		locationScene = locationScene ?: LocationScene(this, arSceneView).apply {
			setMinimalRefreshing(true)
			setOffsetOverlapping(true)
			anchorRefreshInterval = 10000
			locationChangedEvent = DeviceLocationChanged {
				viewModel.currentLocation = it
				coordinatesHolder.text = "${it.latitude} | ${it.longitude} | ${System.currentTimeMillis()}"
			}
		}

		try {
			resumeArElementsTask.run()
		} catch (e: CameraNotAvailableException) {
			Toast.makeText(this, "Unable to get camera", Toast.LENGTH_LONG).show()
			finish()
			return
		}
	}

	private fun processPlaces(places: List<Place>) {
		areAllMarkersLoaded = false
		locationScene?.clearMarkers()

		setupAndRenderPlacesMarkers(places)
		updatePlacesMarkers()
	}

	private fun setupAndRenderPlacesMarkers(places: List<Place>) {
		places.forEach { place ->
			val completableFutureViewRenderable = ViewRenderable.builder()
				.setView(this, R.layout.view_place_label)
				.build()
			CompletableFuture.anyOf(completableFutureViewRenderable)
				.handle<Any> { _, throwable ->
					//here we know the renderable was built or not
					if (throwable != null) {
						// handle renderable load fail
						return@handle null
					}
					val placeMarker = LocationMarker(
						place.position.lng,
						place.position.lat,
						setPlaceNode(place, completableFutureViewRenderable)
					)
					arHandler.postDelayed({
						attachMarkerToScene(
							placeMarker,
							completableFutureViewRenderable.get().view
						)
						if (places.indexOf(place) == places.size - 1) {
							areAllMarkersLoaded = true
						}
					}, 200)
					null
				}
		}
	}

	private fun setPlaceNode(place: Place, completableFuture: CompletableFuture<ViewRenderable>): Node {
		val node = Node().apply {
			renderable = completableFuture.get()
		}

		completableFuture.get().view.apply {
			nameTV.text = place.name
			placeContainer.visibility = View.GONE
			setOnTouchListener { _, _ ->
				Toast.makeText(this@MainActivity, place.name, Toast.LENGTH_SHORT).show()
				performClick()
				false
			}
		}

		return node
	}

	private fun attachMarkerToScene(locationMarker: LocationMarker, layoutRenderable: View) {
		resumeArElementsTask.run {
			locationMarker.scalingMode = LocationMarker.ScalingMode.FIXED_SIZE_ON_SCREEN
			locationMarker.scaleModifier = INITIAL_MARKER_SCALE_MODIFIER

			locationScene?.mLocationMarkers?.add(locationMarker)
			locationMarker.anchorNode?.isEnabled = true

			arHandler.post {
				locationScene?.refreshAnchors()
				layoutRenderable.placeContainer.visibility = View.VISIBLE
			}
		}
		locationMarker.setRenderEvent { locationNode ->
			layoutRenderable.distanceTV.text = AugmentedRealityLocationUtils.showDistance(locationNode.distance)
			resumeArElementsTask.run {
				computeNewScaleModifierBasedOnDistance(locationMarker, locationNode.distance)
			}
		}
	}

	private fun computeNewScaleModifierBasedOnDistance(locationMarker: LocationMarker, distance: Int) {
		val scaleModifier = AugmentedRealityLocationUtils.getScaleModifierBasedOnRealDistance(distance)
		return if (scaleModifier == INVALID_MARKER_SCALE_MODIFIER) {
			detachMarker(locationMarker)
		} else {
			locationMarker.scaleModifier = scaleModifier
		}
	}

	private fun detachMarker(locationMarker: LocationMarker) {
		with(locationMarker) {
			anchorNode?.anchor?.detach()
			anchorNode?.isEnabled = false
			anchorNode = null
		}
	}

	private fun updatePlacesMarkers() {
		arSceneView.scene.addOnUpdateListener()
		{
			if (!areAllMarkersLoaded) {
				return@addOnUpdateListener
			}

			locationScene?.mLocationMarkers?.forEach { locationMarker ->
				locationMarker.height =
					AugmentedRealityLocationUtils.generateRandomHeightBasedOnDistance(
						locationMarker?.anchorNode?.distance ?: 0
					)
			}


			val frame = arSceneView?.arFrame ?: return@addOnUpdateListener
			if (frame.camera.trackingState != TrackingState.TRACKING) {
				return@addOnUpdateListener
			}
			locationScene!!.processFrame(frame)
		}
	}
}