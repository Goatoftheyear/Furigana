package com.example.furigana.ui.theme.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.furigana.ui.theme.viewmodel.GreetingViewModel

@Composable
fun CapturedImage(
    viewModel: GreetingViewModel = viewModel()
) {
    val bitmap = viewModel.imageBitmap.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            modifier = Modifier
                .fillMaxSize(),
            bitmap = bitmap.value.asImageBitmap(),
            contentDescription = "Image Result"
        )
        Box(Modifier.height(400.dp).fillMaxWidth()) {
            Text("Hi")
        }
    }
}