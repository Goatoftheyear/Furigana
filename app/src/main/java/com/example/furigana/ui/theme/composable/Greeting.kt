package com.example.furigana.ui.theme.composable

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.Rect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.furigana.ui.theme.viewmodel.GreetingViewModel
import androidx.core.graphics.createBitmap

@Composable
fun Greeting(
    modifier: Modifier = Modifier,
    onNavigateResult: () -> Boolean,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    viewModel: GreetingViewModel = viewModel(),
) {
    // Preview shows the camera be4 taking
    // preview is just a canvas, need sth to paint on it
    // need check if permission aren't granted
    // Camera Controller
    // ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val windowInfo = LocalWindowInfo.current
    viewModel.imageAnalyzer.clearAnalyzer()

    //TODO: put code below into viewModel
//    var box: Array<out Point?>? = Array<Point>(4) { Point() }
    val box = remember {mutableStateOf(Array<Point>(4) { Point() })}
    val rect = remember {mutableStateOf(Rect().toComposeRect())}
    // TODO: end of code
    val surfaceRequest by viewModel.surfaceRequest.collectAsStateWithLifecycle()
    var bitMapInMemory= remember { mutableStateOf(createBitmap(100, 100)) }
    var taken = remember {mutableStateOf(false)}
    val cameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
    val permission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { res -> println(res) }
    )
    val isProcessing = viewModel.isProcessing.collectAsState()
    val pathState = viewModel.path.collectAsState()

//    DisposableEffect(Unit) {
//        val orientationEventListener =
//            object : OrientationEventListener(context) {
//                override fun onOrientationChanged(orientation: Int) {
//                    if (orientation == ORIENTATION_UNKNOWN) return
//
//                    val rotation = when (orientation) {
//                        in 45 until 135 -> Surface.ROTATION_270
//                        in 135 until 225 -> Surface.ROTATION_180
//                        in 225 until 315 -> Surface.ROTATION_90
//                        else -> Surface.ROTATION_0
//                    }
//                    viewModel.imageCapture.targetRotation = rotation
//                }
//            }
//            orientationEventListener.enable()
//            onDispose { orientationEventListener.disable() }
//        }

//    DisposableEffect(Unit) {
//        val listener = object : OrientationEventListener(context) {
//            override fun onOrientationChanged(orientation: Int) {
//                if (orientation == UNKNOWN_ORIENTATION) return
//
//                // Map degrees to Surface rotation constants
//                val rotation = when (orientation) {
//                    in 45 until 135 -> Surface.ROTATION_270
//                    in 135 until 225 -> Surface.ROTATION_180
//                    in 225 until 315 -> Surface.ROTATION_90
//                    else -> Surface.ROTATION_0
//                }
//                // Update your ImageCapture use case
//                imageCapture.targetRotation = rotation
//            }
//        }
//        listener.enable()
//        onDispose { listener.disable() }
//    }

    LaunchedEffect(lifecycleOwner) {
        viewModel.bindToCamera(context.applicationContext, lifecycleOwner)
    }
    if (cameraPermission == PackageManager.PERMISSION_GRANTED) {
        surfaceRequest?.let { request ->
            CameraXViewfinder(
                surfaceRequest = request,
                modifier = Modifier.fillMaxSize()
            )
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Button(
                    onClick = {
                        viewModel.imageCapture.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onError(exception: ImageCaptureException) {
                                    super.onError(exception)
                                    println("error here")
                                }

                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val bitmapImage = image.toBitmap()
                                    super.onCaptureSuccess(image)
                                        val matrix = Matrix().apply {
                                            postRotate(image.imageInfo.rotationDegrees.toFloat())
                                        }
                                    viewModel.setBitmap(Bitmap.createBitmap(
                                        bitmapImage, 0, 0, bitmapImage.width,
                                        bitmapImage.height, matrix, true
                                    ))
                                    viewModel.startRecognizerProcess(windowInfo.containerSize.width.toFloat() ,windowInfo.containerSize.height.toFloat())
                                    onNavigateResult()


                                }
                            }
                        )
                    }
                ) {
                    Text(
                        text = "Hmmage",
                        textAlign = TextAlign.Center
                    )

                }

            }
        }
    } else {
            Column(verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Camera is required to use this app",
                    textAlign = TextAlign.Center
                )
                    Button(
                        onClick = {
                            permission.launch(Manifest.permission.CAMERA)
                        }
                    ) {
                        Text(
                            text = "Allow",
                            textAlign = TextAlign.Center
                        )
                    }
            }
    }
}
