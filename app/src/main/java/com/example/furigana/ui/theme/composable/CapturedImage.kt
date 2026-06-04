package com.example.furigana.ui.theme.composable

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.furigana.ui.theme.viewmodel.GreetingViewModel

@Composable
fun CapturedImage(
    viewModel: GreetingViewModel = viewModel()
) {
//    val bitmap = viewModel.imageBitmap.collectAsState()
//    Box(modifier = Modifier.fillMaxSize()) {
//        Image(
//            modifier = Modifier
//                .fillMaxSize(),
//            bitmap = bitmap.value.asImageBitmap(),
//            contentDescription = "Image Result"
//        )
//        Box(Modifier.height(400.dp).fillMaxWidth()) {
//            Text("Hi")
//        }
//    }
}