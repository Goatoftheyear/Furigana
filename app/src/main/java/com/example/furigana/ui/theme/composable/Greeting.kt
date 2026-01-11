package com.example.furigana.ui.theme.composable

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.furigana.ui.theme.viewmodel.GreetingViewModel
import com.google.common.util.concurrent.ListenableFuture
import org.intellij.lang.annotations.JdkConstants

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
    LaunchedEffect(lifecycleOwner) {
        viewModel.bindToCamera(context.applicationContext, lifecycleOwner)
    }
    if (cameraPermission == PackageManager.PERMISSION_GRANTED) {
        surfaceRequest?.let { request ->
            CameraXViewfinder(
                surfaceRequest = request,
                modifier = modifier
            )
            Button(
                onClick = {
                    permission.launch(Manifest.permission.CAMERA)
                }
            ) {
                Text(
                    text = "Hmmage",
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Box(Modifier.fillMaxSize(),
            Alignment.Center) {
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = "Camera is required to use this app",
                    textAlign = TextAlign.Center
                )
            Row(horizontalArrangement = Arrangement.Center) {
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
    }
}
