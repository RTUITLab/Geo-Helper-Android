package com.rtuitlab.geohelper

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.ViewRenderable
import com.rtuitlab.geohelper.utils.AugmentedRealityLocationUtils.INITIAL_MARKER_SCALE_MODIFIER
import com.rtuitlab.geohelper.utils.AugmentedRealityLocationUtils.INVALID_MARKER_SCALE_MODIFIER
import com.rtuitlab.geohelper.models.Place
import com.rtuitlab.geohelper.utils.AugmentedRealityLocationUtils
import com.rtuitlab.geohelper.utils.hasPermissions
import com.rtuitlab.geohelper.utils.showDialogOK
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.view_place_label.view.*
import uk.co.appoly.arcorelocation.LocationMarker
import uk.co.appoly.arcorelocation.LocationScene
import uk.co.appoly.arcorelocation.sensor.DeviceLocationChanged
import java.util.concurrent.CompletableFuture

class MainActivity : AppCompatActivity() {

	companion object {
		const val REQUEST_ID_MULTIPLE_PERMISSIONS = 1
		val PERMISSIONS = arrayOf(
			Manifest.permission.ACCESS_FINE_LOCATION,
			Manifest.permission.CAMERA,
			Manifest.permission.ACCESS_COARSE_LOCATION
		)
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

	private var isPermissionsGranted = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		viewModel.locationDataLiveData.observe(this, Observer {
			processPlaces(it)
		})

		isPermissionsGranted = hasPermissions(*PERMISSIONS)
		if (!isPermissionsGranted) {
			ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_ID_MULTIPLE_PERMISSIONS)
		}
	}

	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<out String>,
		grantResults: IntArray
	) {
		if (requestCode == REQUEST_ID_MULTIPLE_PERMISSIONS) {
			if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
				isPermissionsGranted = true
			} else {
				if (PERMISSIONS.any {
						ActivityCompat.shouldShowRequestPermissionRationale(this, it)
					}) {
					showDialogOK(
						getString(R.string.permissions_required),
						DialogInterface.OnClickListener { _, which ->
							when (which) {
								DialogInterface.BUTTON_POSITIVE -> {
									ActivityCompat.requestPermissions(
										this,
										PERMISSIONS, REQUEST_ID_MULTIPLE_PERMISSIONS
									)
								}
								DialogInterface.BUTTON_NEGATIVE -> {
									finish()
								}
							}
						})
				} else {
					Toast.makeText(
						this,
						getString(R.string.setting_for_permissions),
						Toast.LENGTH_LONG
					).show()
					finish()
				}
			}
		}
	}

	override fun onResume() {
		super.onResume()
		if (isPermissionsGranted) {
			setupSession()
		}
	}

	override fun onPause() {
		super.onPause()
		if (isPermissionsGranted) {
			pauseARElements()
		}
	}

	// Pause AR when app is hided
	private fun pauseARElements() {
		arSceneView.session?.let {
			locationScene?.pause()
			arSceneView?.pause()
		}
	}

	// Setup AR session
	private fun setupSession() {
		arSceneView ?: return
		arSceneView.session ?: run {
			try {
				AugmentedRealityLocationUtils.setupSession(this, arCoreInstallRequested)?.let {
					arSceneView.setupSession(it)
				} ?: run {
					arCoreInstallRequested = true
					return
				}
			} catch (e: UnavailableException) {
				AugmentedRealityLocationUtils.handleSessionException(this, e)
			}
		}

		// Create new LocationScene if it was not created early
		locationScene = locationScene ?: LocationScene(this, arSceneView).apply {
			setMinimalRefreshing(true)
			setOffsetOverlapping(true)
			anchorRefreshInterval = 10000
			locationChangedEvent = DeviceLocationChanged {
				viewModel.currentLocation = it
			}
		}

		try {
			resumeArElementsTask.run()
		} catch (e: CameraNotAvailableException) {
			Toast.makeText(this, getString(R.string.unable_camera), Toast.LENGTH_LONG).show()
			finish()
			return
		}
	}

	// Process new places which were got from server
	private fun processPlaces(places: List<Place>) {
		areAllMarkersLoaded = false
		locationScene?.clearMarkers()

		setupAndRenderPlacesMarkers(places)
		updatePlacesMarkers()
	}

	// Setup new markers
	private fun setupAndRenderPlacesMarkers(places: List<Place>) {
		places.forEach { place ->
			val completableFutureViewRenderable = ViewRenderable.builder()
				.setView(this, R.layout.view_place_label)
				.build()
			CompletableFuture.anyOf(completableFutureViewRenderable)
				.handle<Any> { _, throwable ->
					//here we know the renderable was built or not
					throwable?.let {
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

	// Set new node for marker
	private fun setPlaceNode(place: Place, completableFuture: CompletableFuture<ViewRenderable>): Node {
		val node = Node().apply {
			renderable = completableFuture.get()
		}

		completableFuture.get().view.apply {
			nameTV.text = place.name
			descriptionTV.text = place.description
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
			layoutRenderable.distanceTV.text = AugmentedRealityLocationUtils.showDistance(this, locationNode.distance)
			resumeArElementsTask.run {
				computeNewScaleModifierBasedOnDistance(locationMarker, locationNode.distance)
			}
		}
	}

	// Compute size of marker based on distance
	private fun computeNewScaleModifierBasedOnDistance(locationMarker: LocationMarker, distance: Int) {
		return AugmentedRealityLocationUtils.getScaleModifierBasedOnRealDistance(distance).takeIf {
			it != INVALID_MARKER_SCALE_MODIFIER
		}?.let {
			locationMarker.scaleModifier = it
		} ?:run {
			detachMarker(locationMarker)
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