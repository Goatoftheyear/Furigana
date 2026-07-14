package com.example.furigana.ui.theme.composable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.furigana.ui.theme.viewmodel.ImageToResultViewModel
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapturedImage(
    viewModel: ImageToResultViewModel = viewModel()
) {
    val bitmap = viewModel.imageBitmap.collectAsState()
    val isProcessing = viewModel.isProcessing.collectAsState()
    val path = viewModel.path.collectAsState()
    val paragraph = viewModel.paragraph.collectAsState()
    val showSheet = remember { MutableStateFlow(true) }
    val showSheetState = showSheet.collectAsState()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            modifier = Modifier
                .fillMaxSize(),
            bitmap = bitmap.value.asImageBitmap(),
            contentDescription = "Image Result"
        )
    }
    if (isProcessing.value) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
    }
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (p in path.value) {
                drawPath(
                    path = p,
                    color = Color.Black,
                    style = Stroke(width = 10f)
                )
            }
        }
        if (showSheetState.value && !isProcessing.value) {
            ModalBottomSheet(
                onDismissRequest = { showSheet.value = false },
                sheetState = sheetState,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .height(300.dp)
                ) {
                     items(paragraph.value) { textField ->
                        FlowRow() {
                            textField.keys.forEach { it ->
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier.height(18.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(textField[it]!!, fontSize = 14.sp)
                                        }
                                        Text(it, fontSize = 22.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
//        if (!showSheet) {
//            FloatingActionButton(
//                onClick = { showSheet = true },
//                modifier = Modifier
//                    .align(Alignment.BottomEnd)
//                    .navigationBarsPadding()
//        ) {
//            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Show readings")
//        }
//    }


}