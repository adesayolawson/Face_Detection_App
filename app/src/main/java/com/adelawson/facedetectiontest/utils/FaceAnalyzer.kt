package com.adelawson.facedetectiontest.utils

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.Lifecycle
import com.adelawson.facedetectiontest.ui.utils.FaceContourGraphic
import com.adelawson.facedetectiontest.ui.utils.GraphicOverlay
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.nio.ByteBuffer

typealias FaceDetectionListener = (luminosity: Double, faces: List<Face>) -> Unit


class FaceAnalyzer(
    private val graphicOverlay: GraphicOverlay,
    private val lifecycle: Lifecycle,
    private val faceDetectionListener: FaceDetectionListener,
) : ImageAnalysis.Analyzer {
    private val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .build()
    private val faceDetector = FaceDetection.getClient(faceDetectorOptions)

    init {
        lifecycle.addObserver(faceDetector)
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }

    @ExperimentalGetImage
    override fun analyze(image: ImageProxy) {

        //calculate luminosity to determine brightness of the face
        val buffer = image.planes[0].buffer
        val data = buffer.toByteArray()
        val pixels = data.map { it.toInt() and 0xFF }
        val luminosity = pixels.average()


        //perform facial analysis

        val faceAnalysisImage = image.image!!


        faceDetector.process(
            InputImage.fromMediaImage(
                faceAnalysisImage, image.imageInfo.rotationDegrees
            )
        ).addOnSuccessListener { faces ->
            graphicOverlay.clear()
            faceDetectionListener(luminosity, faces)
            for (face in faces) {
                val faceBoundBoxGraphic =
                    FaceContourGraphic(graphicOverlay, face, faceAnalysisImage.cropRect)
                graphicOverlay.add(faceBoundBoxGraphic)
            }
            graphicOverlay.postInvalidate()
            image.close()
        }.addOnFailureListener {
            image.close()
        }


    }
}