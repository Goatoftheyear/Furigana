package com.example.furigana.ui.theme.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.util.Size
import androidx.camera.core.AspectRatio.RATIO_16_9
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.ui.graphics.Path
import androidx.core.graphics.createBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import com.atilika.kuromoji.ipadic.Tokenizer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

data class TokenResult(
    val surface: String,
    val reading: String,
)

class ImageToResultViewModel(application: Application) : AndroidViewModel(application) {

    private val _box = MutableStateFlow(Array<Point>(4) { Point() })
    private val _imageBitmap = MutableStateFlow(createBitmap(100, 100))
    val imageBitmap = _imageBitmap.asStateFlow()
    private val _tokens = MutableStateFlow<List<List<TokenResult>>>(emptyList())
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

    //TODO: use processing
    val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()
    private val _results = MutableStateFlow<List<String>>(mutableListOf())
    val results = _results.asStateFlow()

    //TODO use this 1 for result, get ans from kuromuji, put value if its kanji and loop the keys
    private val _furiganaResults = MutableStateFlow<Map<String, String>>(mutableMapOf())
    val furiganaResults = _furiganaResults.asStateFlow()
    val hiraganaRegex = Regex("\\p{Script=Hiragana}+")
    val katakanaRegex = Regex("\\p{Script=Katakana}+")
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
        .setResolutionStrategy(
            ResolutionStrategy(
                Size(1980, 1080),
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
            )
        )
        .build()
    val imageCapture: ImageCapture = ImageCapture.Builder()
        .setResolutionSelector(resolutionSelector)
        .build()

    val imageAnalyzer = ImageAnalysis.Builder()
        .setResolutionSelector(resolutionSelector)
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

    fun setFuriganaResults(results: MutableMap<String, String>) {
        _furiganaResults.update {
            results
        }
    }

    fun startRecognizerProcess(screenWidth: Float, screenHeight: Float) {
        setIsProcessing(true)
        recognizer.process(InputImage.fromBitmap(_imageBitmap.value, 0))
            .addOnSuccessListener { text ->
                val currentResults: MutableList<String> = mutableListOf()
                val furiganaOutput: MutableMap<String, String> = mutableMapOf()
                val ans = text.textBlocks
                val paths = mutableListOf<Path>()
                for (line in ans) {
                    _box.value = line.cornerPoints!!
                    //TODO: kept it for experimentation
//                    rect.value = line.boundingBox!!.toComposeRect()

                    val scaleHorizontal = screenWidth / _imageBitmap.value.width
                    val scaleVertical = screenHeight / _imageBitmap.value.height
                    var prev = Point(
                        line.cornerPoints!![0].x,
                        line.cornerPoints!![0].y
                    )
                    var findLongestSide = mutableMapOf<String, Float>()
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
                    var startTextPointReference = mutableMapOf<String, Point>()
                    if (findLongestSide["x"]!! > findLongestSide["y"]!!) {
                        startTextPointReference.put("horizontal", line.cornerPoints!![0])
                    } else {
                        startTextPointReference.put("horizontal", line.cornerPoints!![1])

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
                    val lineText = line.text
                    val tokens = tokenizer.tokenize(lineText)
                    currentResults.add("test ${line.text}")
                    for (token in tokens) {
                        val furigana = token.allFeaturesArray[token.allFeaturesArray.lastIndex]
                        println("test ${hiraganaRegex.matches(token.surface)}")
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
                        currentResults.add(token.surface + "\t" + furiganaHiragana)
                        furiganaOutput.put(token.surface, furiganaHiragana)
                    }
                }
                setPath(paths)
                setIsProcessing(false)
                setFuriganaResults(furiganaOutput)
            }
            .addOnFailureListener { setIsProcessing(false) }
    }
}