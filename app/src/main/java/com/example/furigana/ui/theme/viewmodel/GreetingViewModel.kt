package com.example.furigana.ui.theme.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Point
import androidx.camera.core.AspectRatio.RATIO_16_9
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.ui.graphics.Path
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.Executors

class GreetingViewModel(application: Application) : AndroidViewModel(application) {

//    private val context = getApplication<Application>().applicationContext

    //    val orientationEventListener =
//        object : OrientationEventListener(context) {
//            override fun onOrientationChanged(orientation: Int) {
//                if (orientation == ORIENTATION_UNKNOWN) return
//
//                val rotation = when (orientation) {
//                    in 45 until 135 -> Surface.ROTATION_270
//                    in 135 until 225 -> Surface.ROTATION_180
//                    in 225 until 315 -> Surface.ROTATION_90
//                    else -> Surface.ROTATION_0
//                }
//                imageCapture.targetRotation = rotation
//            }
//        }
    private val _box = MutableStateFlow(Array<Point>(4) { Point() })
    val box = _box.asStateFlow()

    private var _path = MutableStateFlow( mutableListOf(Path().apply {
        _box.value.mapIndexed { index, point ->
            if (index == 0) {
                println("move ${point.x} ${point.y}")
                moveTo(point.x.toFloat(), point.y.toFloat())
            } else {
                println("line ${point.x} ${point.y}")
                lineTo(point.x.toFloat(), point.y.toFloat())
            }
        }
        close()
    }))
    val path = _path.asStateFlow()
    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    private var surfaceMeteringPointFactory: SurfaceOrientedMeteringPointFactory? = null
    val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest.asStateFlow()
    val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())


    fun setBitmap(bitmap: Bitmap) {
        _imageBitmap.update {
            bitmap
        }
    }

    fun setPath(path: MutableList<Path>) {
        _path.update {
            path
        }
    }
    private val cameraPreviewUseCase = Preview.Builder().build().apply {
        setSurfaceProvider { newSurfaceRequest ->
            _surfaceRequest.update {
                newSurfaceRequest

            }
            surfaceMeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                newSurfaceRequest.resolution.width.toFloat(),
                newSurfaceRequest.resolution.height.toFloat()
            )
        }
    }


    val imageCapture: ImageCapture = ImageCapture.Builder()
//        .setResolutionSelector(
//            ResolutionSelector.Builder()
//                .setResolutionStrategy(                ResolutionStrategy(
//                    Size(1980, 1080),
//                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
//                .build()
//        )
        //TODO: fix deprecation
        .setTargetAspectRatio(RATIO_16_9)
        .build()
    val cameraExecutor = Executors.newSingleThreadExecutor()

    val imageAnalyzer = ImageAnalysis.Builder()
        .setTargetAspectRatio(RATIO_16_9)
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

    suspend fun bindToCamera(appContext: Context, lifecycleOwner: LifecycleOwner) {
        val processCameraProvider = ProcessCameraProvider.awaitInstance(appContext)
        processCameraProvider.bindToLifecycle(
            lifecycleOwner, DEFAULT_BACK_CAMERA, imageCapture, imageAnalyzer, cameraPreviewUseCase
        )
        try {
            awaitCancellation()
        } finally {
            processCameraProvider.unbindAll()
        }
    }
}