package com.adelawson.facedetectiontest.ui.fragments.cameraCapture

import android.Manifest
import android.graphics.*
import android.media.MediaActionSound
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.adelawson.facedetectiontest.utils.FaceAnalyzer
import com.adelawson.facedetectiontest.R
import com.adelawson.facedetectiontest.databinding.FragmentCameraCaptureBinding
import com.google.mlkit.vision.face.Face
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.absoluteValue


class CameraCaptureFragment : Fragment() {

    companion object {
        fun newInstance() = CameraCaptureFragment()
        const val DESIRED_WIDTH_CROP_PERCENT = 5
        const val DESIRED_HEIGHT_CROP_PERCENT = 170
    }

    private lateinit var imageCapture: ImageCapture
    private val viewModel by viewModels<CameraCaptureViewModel>()
    private lateinit var binding: FragmentCameraCaptureBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var screenRectangle: RectF
    private val permissionRequestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        //check if there is any permission that has not been granted and perform the operation
        //that requires the permission
        val isCameraPermissionAccepted = permissions[Manifest.permission.CAMERA]
        val isAudioRecordingAccepted = permissions[Manifest.permission.RECORD_AUDIO]
        if (isCameraPermissionAccepted == true && isAudioRecordingAccepted == true) {
            startCamera()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCameraCaptureBinding.inflate(inflater)
        cameraExecutor = Executors.newSingleThreadExecutor()
        initUI()
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun initUI() {
        binding.overlay.apply {
            setZOrderOnTop(true)
            holder.setFormat(PixelFormat.TRANSPARENT)
            holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(p0: SurfaceHolder) {
                    holder?.let {
                        drawOverlay(
                            it
                        )
                    }
                }

                override fun surfaceChanged(
                    p0: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {

                }

                override fun surfaceDestroyed(p0: SurfaceHolder) {

                }
            })
        }
        //enable button on start so on click user can start the preview
        binding.isAllowedToTakePhoto = true
        binding.floatingActionButton.setOnClickListener {
            checkPermissions()
        }
    }


    private fun checkPermissions() {
        val permissionList = mutableListOf<String>()
        permissionList.add(Manifest.permission.CAMERA)
        permissionList.add(Manifest.permission.RECORD_AUDIO)
        permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE)

        permissionRequestLauncher.launch(permissionList.toTypedArray())


    }

    private fun startCamera() {
        binding.floatingActionButton.setOnClickListener {
            capture()
        }
        binding.viewFinder.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            Runnable {
                try {
                    // Used to bind the lifecycle of cameras to the lifecycle owner
                    val cameraProvider = cameraProviderFuture.get()

                    // Preview
                    val preview = Preview.Builder()
                        .build().also {
                            it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                        }
                    // Select back camera
                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()


                    imageCapture = ImageCapture.Builder()
                        .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                        .setJpegQuality(40)
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .build()

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setImageQueueDepth(1)
                        .build()
                        .also {
                            it.setAnalyzer(
                                cameraExecutor,
                                FaceAnalyzer(
                                    binding.graphicOverlay,
                                    this.lifecycle
                                ) { luminosity, faces ->

                                    Log.d(
                                        "luminosityMeasurement",
                                        "Average luminosity: $luminosity"
                                    )
                                    Log.d("faces", "FaceData: $faces")
                                    if (faces.isNotEmpty()){
                                        handleFaceDetected(faces, luminosity)
                                    }else{
                                        binding.isAllowedToTakePhoto = false
                                        drawOverlay(binding.overlay.holder)
                                    }

                                })
                        }

                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        viewLifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture,
                        imageAnalyzer
                    )
                    preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                    Log.d("camera state", "${camera.cameraInfo}")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    private fun handleFaceDetected(faces: List<Face>, luminosity: Double) {


        //get the closest face by the height of the bound box
        val face = if (faces.size > 1) faces.maxBy { it.boundingBox.height() } else faces[0]
        val boundBox = RectF(face.boundingBox)


        //checks head tilt up or down
        val rotationX = face.headEulerAngleX.absoluteValue
        //checks whether user is looking towards or away from the camera
        val rotationY = face.headEulerAngleY.absoluteValue
        //checks head tilt left or right
        val rotationZ = face.headEulerAngleZ.absoluteValue
        //only useful when classification mode is set to all
        val rightEyeOpenProbability = face.rightEyeOpenProbability
        val leftEyeOpenProbability = face.leftEyeOpenProbability


        val isLookingForward = (rotationX < 15) && (rotationY < 15) && (rotationZ < 15)
        val isBrightEnough = luminosity > 90
        val isInSquare = (boundBox.left > screenRectangle.left) &&
                (boundBox.top < screenRectangle.top) && (boundBox.bottom > screenRectangle.bottom)
                && (boundBox.right < screenRectangle.right)

        val isFaceLargeEnough =
            ((boundBox.width() * boundBox.height())
                    / (screenRectangle.width() * screenRectangle.height())).absoluteValue >=0.035f
        val isAllowedToTakePhoto = (isLookingForward && isBrightEnough && isInSquare && isFaceLargeEnough)

        binding.isAllowedToTakePhoto = isAllowedToTakePhoto
        drawOverlay(binding.overlay.holder, isAllowedToTakePhoto)
    }

    @Throws(FileNotFoundException::class)
    private fun capture() {
        shootSound()
        //External storage directory as specified.
        val imagePath = File(
            requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES + "/" + "TestImages"),
            "testImg_${Date().time}.jpg"
        ).absolutePath
        val storageFile = File(imagePath)

        if (!storageFile.exists() && !storageFile.mkdirs()) {
            throw FileNotFoundException("Unable to create folder " + storageFile.absolutePath)
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(storageFile).build()

        // Setup image capture listener which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(storageFile)
                    val navAction = CameraCaptureFragmentDirections
                        .actionCameraCaptureFragmentToImagePreviewFragment(savedUri)
                    findNavController().navigate(navAction)

                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        requireContext(),
                        "Error occurred during capture",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun shootSound() {
        val sound = MediaActionSound()
        sound.play(MediaActionSound.SHUTTER_CLICK)
    }

    private fun drawOverlay(
        holder: SurfaceHolder,
        isFacePositionedProperly: Boolean = false
    ) {

        val canvas = holder.lockCanvas() ?: return
        val bgPaint = Paint().apply {
            alpha = 140
        }
        val heightCropPercent = DESIRED_HEIGHT_CROP_PERCENT
        val widthCropPercent = DESIRED_WIDTH_CROP_PERCENT
        canvas.drawPaint(bgPaint)
        val rectPaint = Paint()
        rectPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        rectPaint.style = Paint.Style.FILL
        rectPaint.color = Color.WHITE
        val outlinePaint = Paint()
        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.color = if (isFacePositionedProperly) Color.GREEN else Color.WHITE
        outlinePaint.strokeWidth = 4f
        val surfaceWidth = holder.surfaceFrame.width()
        val surfaceHeight = holder.surfaceFrame.height()

        val cornerRadius = 25f
        // Set rect centered in frame
        val rectTop = surfaceHeight * heightCropPercent / 2 / 100f
        val rectLeft = surfaceWidth * widthCropPercent / 2 / 100f
        val rectRight = surfaceWidth * (1 - widthCropPercent / 2 / 100f)
        val rectBottom = surfaceHeight * (1 - heightCropPercent / 2 / 100f)
        screenRectangle = RectF(rectLeft, rectTop, rectRight, rectBottom)
        canvas.drawRoundRect(
            screenRectangle, cornerRadius, cornerRadius, rectPaint
        )
        canvas.drawRoundRect(
            screenRectangle, cornerRadius, cornerRadius, outlinePaint
        )
        val textPaint = Paint()
        textPaint.color = Color.WHITE
        textPaint.textSize = 25F

        val overlayText = getString(R.string.overlay_help)
        val textBounds = Rect()
        textPaint.getTextBounds(overlayText, 0, overlayText.length, textBounds)
        val textX = (surfaceWidth - textBounds.width()) / 2f
        val textY = rectBottom + textBounds.height() + 15f // put text below rect and 15f padding
        canvas.drawText(
            getString(R.string.overlay_help),
            textX,
            textY,
            textPaint
        )
        holder.unlockCanvasAndPost(canvas)
    }


}