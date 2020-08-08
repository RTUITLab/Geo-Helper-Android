package com.rtuitlab.geohelper

import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3

class PlaceNode: AnchorNode() {
    override fun onUpdate(p0: FrameTime?) {
        val cameraPosition = scene?.camera?.worldPosition ?: return
        val direction = Vector3.subtract(cameraPosition, this.worldPosition)
        val lookRotation = Quaternion.lookRotation(direction, Vector3.up())
        this.worldRotation = lookRotation
    }
}