package com.example.furigana.ui.theme.composable

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.view.OrientationEventListener
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.ExperimentalGetImage
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
//import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atilika.kuromoji.ipadic.Tokenizer
import com.example.furigana.ui.theme.viewmodel.GreetingViewModel
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.core.graphics.createBitmap
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import kotlin.math.abs

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
    val tokenizer = Tokenizer()

//    val tokens = tokenizer.tokenize("お寿司が食べたい。")
//    for (token in tokens) {
//        println(token.getSurface() + "\t" + token.getAllFeatures())
//    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    var imageProxy:ImageProxy
    val boxState = viewModel.box.collectAsState()
    val pathState = viewModel.path.collectAsState()
//    var path = Path().apply {
//        box.value.mapIndexed { index, point ->
//                if (index == 0) {
//                    println("move ${point.x} ${point.y}")
//                    moveTo( point.x.toFloat(),  point.y.toFloat())
//                } else {
//                    println("line ${point.x} ${point.y}")
//                    lineTo(point.x.toFloat(), point.y.toFloat())
//                }
//        }
//        close()
//    }
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

    println("width ${bitMapInMemory.value.width}")
    if (taken.value) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                modifier = Modifier
                    .fillMaxSize(),
                bitmap = bitMapInMemory.value.asImageBitmap(),
                contentDescription = "hi"
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                pathState.value.forEach {
                    drawPath(
                        path = it,
                        color = Color.Black,
                        style = Stroke(width = 10f)
                    )
                }
            }
        }
    }
    else if (cameraPermission == PackageManager.PERMISSION_GRANTED) {
        surfaceRequest?.let { request ->
            CameraXViewfinder(
                surfaceRequest = request,
                modifier = Modifier.fillMaxSize()
            )
//            Canvas(modifier = Modifier.fillMaxSize()) {
//                pathState.value.forEach {
//                    drawPath(
//                        path = it,
//                        color = Color.Black,
//                        style = Stroke(width = 10f)
//                    )
//                }
//                println(rect.value)
//                println(rect.value.topLeft)
//                println(rect.value.size)
//            }
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
                                    taken.value = true
                                    val bitmapImage = image.toBitmap()
                                    super.onCaptureSuccess(image)

                                        val matrix = Matrix().apply {
                                            postRotate(image.imageInfo.rotationDegrees.toFloat())
                                        }
                                                bitMapInMemory.value = Bitmap.createBitmap(
                                                    bitmapImage, 0, 0, bitmapImage.width,
                                                    bitmapImage.height, matrix, true
                                                )
//                                    viewModel.imageAnalyzer.setAnalyzer(executor) { analysisImage ->
//                                        image.image?.let {
//                                                image ->
//                                            println("test image rotation ${analysisImage.imageInfo.rotationDegrees}")
//                                            val img = InputImage.fromMediaImage(
//                                                image,
//                                                analysisImage.imageInfo.rotationDegrees
//                                            )
//                                            recognizer.process(img).addOnSuccessListener {
//                                                text ->
//                                                pathState.value = Path().apply {
//                                                    box.value.mapIndexed { index, point ->
//                                                        if (index == 0) {
////                                                            println("move ${point.x} ${point.y}")
//                                                            moveTo( point.x.toFloat()/2,  point.y.toFloat()/2)
//                                                        } else {
//                                                            lineTo(point.x.toFloat()/2, point.y.toFloat()/2)
//                                                        }
//                                                    }
//                                                    close()
//                                                }
//                                                val ans = text.textBlocks
//                                                for (line in ans) {
//                                                    println("test cornerPoints ${line.cornerPoints}")
//                                                    println("test boundingBox${line.boundingBox}")
//
//                                                    println("test top ${line.boundingBox?.top}")
//                                                    println("test bottom ${line.boundingBox?.bottom}")
//                                                    println("test left ${line.boundingBox?.left}")
//                                                    println("test right ${line.boundingBox?.right}")
//                                                }
//                                                analysisImage.close()
//                                            }
//                                        }
//                                    }
//                                    bitMapInMemory.value = image.toBitmap()
//                                    recognizer.process(bitMapInMemory.value, 0)
                                    recognizer.process(InputImage.fromBitmap(bitMapInMemory.value, 0))
                                        .addOnSuccessListener { text ->
                                            val ans = text.textBlocks
                                            val paths = mutableListOf<Path>()
                                            for (line in ans) {
                                                box.value = line.cornerPoints!!
                                                rect.value = line.boundingBox!!.toComposeRect()

                                                val scaleHorizontal = windowInfo.containerSize.width.toFloat() / image.toBitmap().width
                                                val scaleVertical = windowInfo.containerSize.height.toFloat() / image.toBitmap().height
                                                var prev = Point(
                                                    line.cornerPoints!![0].x,
                                                    line.cornerPoints!![0].y
                                                )
                                                var findLongestSide = mutableMapOf<String, Float>()
                                                findLongestSide.put("x", 0.0f)
                                                findLongestSide.put("y", 0.0f)
                                                line.cornerPoints!!.map {
                                                    if( abs(it.x - prev.x)  > findLongestSide.get("x")!! ) {
                                                        findLongestSide.put("x",
                                                            abs(it.x - prev.x).toFloat()
                                                        )
                                                    }
                                                    if( abs(it.y - prev.y)  > findLongestSide.get("y")!! ) {
                                                        findLongestSide.put("y",
                                                            abs(it.y - prev.y).toFloat()
                                                        )
                                                    }
                                                    prev = it
                                                }
                                                var startTextPointReference = mutableMapOf<String, Point>()
                                                if(findLongestSide["x"]!! > findLongestSide["y"]!!) {
                                                    startTextPointReference.put("horizontal", line.cornerPoints!![0])
                                                } else {
                                                    startTextPointReference.put("horizontal", line.cornerPoints!![1])

                                                }
                                                val path = Path().apply {
                                                    line.cornerPoints!!.mapIndexed { index, point ->

                                                        if (index == 0) {
                                                            println("move ${point.x * scaleHorizontal} ${point.y * scaleVertical}")
                                                            moveTo( point.x.toFloat() * scaleHorizontal,  point.y.toFloat() * scaleVertical)
                                                        } else {
                                                            println("move ${point.x * scaleHorizontal} ${point.y * scaleVertical}")
                                                            lineTo(point.x.toFloat() * scaleHorizontal, point.y.toFloat()  * scaleVertical)
                                                        }
                                                    }
                                                    close()
                                                }
                                                paths.add(path)
//                                                val mutableBitMap = image.toBitmap().copy(Bitmap.Config.ARGB_8888, true)
//                                                val canvas = Canvas(mutableBitMap)
//                                                canvas.drawPath(path, Paint().apply {
//                                                    color = android.graphics.Color.RED
//                                                })
                                                val lineText = line.text
                                                println(lineText)
                                                val tokens = tokenizer.tokenize(lineText)
//                                                for (token in tokens) {
//                                                    println(token.getSurface() + "\t" + token.getAllFeatures())
//                                                }
                                            }
                                            viewModel.setPath(paths)
                                        }
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
