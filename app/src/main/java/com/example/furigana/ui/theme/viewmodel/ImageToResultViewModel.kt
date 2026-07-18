package com.example.furigana.ui.theme.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.util.Size
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.ui.graphics.Path
import androidx.core.graphics.createBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.atilika.kuromoji.ipadic.Tokenizer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.mutableListOf
import kotlin.math.abs

class ImageToResultViewModel(application: Application) : AndroidViewModel(application) {

    private val _box = MutableStateFlow(Array<Point>(4) { Point() })
    private val _imageBitmap = MutableStateFlow(createBitmap(100, 100))
    val imageBitmap = _imageBitmap.asStateFlow()
    private var _path = MutableStateFlow(mutableListOf(Path().apply {
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
    val tokenizer = Tokenizer()
    val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()
    private val _paragraph = MutableStateFlow<List<Map<String, String>>>(mutableListOf())
    val paragraph = _paragraph.asStateFlow()
    val hiraganaRegex = Regex("\\p{Script=Hiragana}+")
    val katakanaRegex = Regex("\\p{Script=Katakana}+")
    val europeanRegex = Regex("""[\s\p{P}]*\p{Script=Latin}+[\s\p{P}]*""")
    var cameraProvider: ProcessCameraProvider? = null;
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
    private val resolutionSelector = ResolutionSelector.Builder()
        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
        .setResolutionStrategy(
            ResolutionStrategy(
                Size(1920, 1080),
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
            )
        )
        .build()
    val imageCapture: ImageCapture = ImageCapture.Builder()
        .setResolutionSelector(resolutionSelector)
        .build()

    fun unbindCamera() {
        cameraProvider?.unbindAll()
    }

    suspend fun bindToCamera(appContext: Context, lifecycleOwner: LifecycleOwner) {
        val processCameraProvider = ProcessCameraProvider.awaitInstance(appContext)
        processCameraProvider.bindToLifecycle(
            lifecycleOwner, DEFAULT_BACK_CAMERA, imageCapture, cameraPreviewUseCase
        )
        cameraProvider = processCameraProvider
        try {
            awaitCancellation()
        } finally {
            processCameraProvider.unbindAll()
        }
    }


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

    fun setIsProcessing(isProcessing: Boolean) {
        _isProcessing.update {
            isProcessing
        }
    }

    fun setFuriganaResults(allTokenResults: List<MutableMap<String, String>>) {
        _paragraph.update {
            allTokenResults
        }
    }

    fun startRecognizerProcess(screenWidth: Float, screenHeight: Float) {
        setFuriganaResults(mutableListOf())
        setPath(mutableListOf())
        setIsProcessing(true)
        recognizer.process(InputImage.fromBitmap(_imageBitmap.value, 0))
            .addOnSuccessListener { text ->
                viewModelScope.launch {
                val ans = text.textBlocks
                val paths = mutableListOf<Path>()
                val allTokenResults = mutableListOf<MutableMap<String, String>>()
                for (line in ans) {
                    val lineText = line.text
                    if (europeanRegex.containsMatchIn(lineText) &&
                        !hiraganaRegex.containsMatchIn(lineText) &&
                        !katakanaRegex.containsMatchIn((lineText))
                    ) continue
                    coroutineScope {
                        async(Dispatchers.Default) {
                            createBoxOnText(line,paths,screenWidth,screenHeight)
                        }
                        async(Dispatchers.Default) {
                            tokenizeLine(lineText,allTokenResults)
                            }
                    }
                    }
                    setFuriganaResults(allTokenResults)
                    setPath(paths)
                }
            }
            .addOnFailureListener { setIsProcessing(false) }
    }
    fun createBoxOnText(line: Text.TextBlock, paths:MutableList<Path>,
                        screenWidth: Float, screenHeight: Float) {
        _box.value = line.cornerPoints!!
        //TODO: kept it for experimentation
//                    rect.value = line.boundingBox!!.toComposeRect()

        val scaleHorizontal = screenWidth / _imageBitmap.value.width
        val scaleVertical = screenHeight / _imageBitmap.value.height
        var prev = Point(
            line.cornerPoints?.get(0)?.x ?: 0,
            line.cornerPoints?.get(0)?.y ?: 0
        )
        val findLongestSide = mutableMapOf<String, Float>()
        findLongestSide.put("x", 0.0f)
        findLongestSide.put("y", 0.0f)
        line.cornerPoints!!.map {
            if (abs(it.x - prev.x) > findLongestSide.get("x")!!) {
                findLongestSide.put(
                    "x",
                    abs(it.x - prev.x).toFloat()
                )
            }
            if (abs(it.y - prev.y) > findLongestSide.get("y")!!) {
                findLongestSide.put(
                    "y",
                    abs(it.y - prev.y).toFloat()
                )
            }
            prev = it
        }
        val startTextPointReference = mutableMapOf<String, Point>()
        if (findLongestSide["x"]!! > findLongestSide["y"]!!) {
            startTextPointReference.put(
                "horizontal",
                line.cornerPoints!![0]
            )
        } else {
            startTextPointReference.put(
                "horizontal",
                line.cornerPoints!![1]
            )

        }
        val path = Path().apply {
            line.cornerPoints!!.mapIndexed { index, point ->

                if (index == 0) {
                    println("move ${point.x * scaleHorizontal} ${point.y * scaleVertical}")
                    moveTo(
                        point.x.toFloat() * scaleHorizontal,
                        point.y.toFloat() * scaleVertical
                    )
                } else {
                    println("move ${point.x * scaleHorizontal} ${point.y * scaleVertical}")
                    lineTo(
                        point.x.toFloat() * scaleHorizontal,
                        point.y.toFloat() * scaleVertical
                    )
                }
            }
            close()
        }
        paths.add(path)
    }
    fun tokenizeLine(lineText: String, allTokenResults: MutableList<MutableMap<String, String>>) {
        val furiganaOutput: MutableMap<String, String> = mutableMapOf()
        val tokens = tokenizer.tokenize(lineText)
        for (token in tokens) {
            if (token.allFeaturesArray.isEmpty()) {
                continue
            }
            val furigana =
                token.allFeaturesArray[token.allFeaturesArray.lastIndex]
            if (hiraganaRegex.matches(token.surface)
                || katakanaRegex.matches(token.surface)
                //check for empty result
                || !katakanaRegex.matches(furigana)
            ) {
                furiganaOutput.put(token.surface, "")
                continue
            }
            val furiganaHiragana = furigana.map { it ->
                (it.code - 0x0060).toChar()
            }.joinToString("")
            //Assume that all kanji with hiragana has only 1 hiragana at the end
            if (hiraganaRegex.containsMatchIn(token.surface)) {
                val kanjiOnly: String = token.surface.dropLast(1)
                val kanjiOnlyFurigana: String = furiganaHiragana.dropLast(1)
                val finalHiraganaCharacter: String =
                    token.surface.last().toString()
                furiganaOutput.put(kanjiOnly, kanjiOnlyFurigana)
                furiganaOutput.put(finalHiraganaCharacter, "")
            } else {
                furiganaOutput.put(token.surface, furiganaHiragana)
            }
        }

        allTokenResults.add(furiganaOutput)
        setIsProcessing(false)
    }
}