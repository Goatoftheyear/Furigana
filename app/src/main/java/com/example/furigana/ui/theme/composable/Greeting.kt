package com.example.furigana.ui.theme.composable

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.furigana.ui.theme.viewmodel.GreetingViewModel
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions

@Composable
fun Greeting(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    viewModel: GreetingViewModel = viewModel()
) {
    // Preview shows the camera be4 taking
    // preview is just a canvas, need sth to paint on it
    // need check if permission aren't granted
    // Camera Controller
    // ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
    val surfaceRequest by viewModel.surfaceRequest.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val cameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
    val permission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { res -> println(res) }
    )
    var bitMapInMemory: Bitmap
    val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    LaunchedEffect(lifecycleOwner) {
        viewModel.bindToCamera(context.applicationContext, lifecycleOwner)
    }
    if (cameraPermission == PackageManager.PERMISSION_GRANTED) {
        surfaceRequest?.let { request ->
            CameraXViewfinder(
                surfaceRequest = request,
                modifier = modifier
            )
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = modifier.fillMaxSize()
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
                                    super.onCaptureSuccess(image)
                                    bitMapInMemory = image.toBitmap()
                                    println("toire")
                                    recognizer.process(bitMapInMemory, 0)
                                        .addOnSuccessListener { text ->
                                            val ans = text.textBlocks
                                            for (line in ans) {
                                                val lineText = line.text
                                                println(lineText)
                                                val tokens = tokenizer.tokenize(lineText)
                                                }
                                            }
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
            modifier = modifier.fillMaxSize()) {
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
