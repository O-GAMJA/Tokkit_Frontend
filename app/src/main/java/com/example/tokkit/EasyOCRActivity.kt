//package com.example.tokkit
//
//import android.app.Activity
//import android.content.Intent
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.net.Uri
//import android.os.Bundle
//import android.provider.MediaStore
//import android.widget.Button
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.appcompat.app.AppCompatActivity
//import kotlinx.coroutines.*
//import org.tensorflow.lite.Interpreter
//import java.io.FileInputStream
//import java.io.InputStream
//import java.nio.MappedByteBuffer
//import java.nio.channels.FileChannel
//
//class EasyOCRActivity : AppCompatActivity(), CoroutineScope by MainScope() {
//
//    private lateinit var detector: Interpreter
//    private lateinit var recognizer: Interpreter
//
//    private lateinit var imageView: ImageView
//    private lateinit var textResult: TextView
//    private lateinit var buttonSelect: Button
//    private lateinit var buttonExtract: Button
//
//    private val PICK_IMAGE_REQUEST = 1001
//    private var selectedBitmap: Bitmap? = null
//
//    private val charset = listOf(
//        '_',
//        'A','B','C','D','E','F','G','H','I','J','K','L','M',
//        'N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
//        'a','b','c','d','e','f','g','h','i','j','k','l','m',
//        'n','o','p','q','r','s','t','u','v','w','x','y','z'
//    )
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_easy_ocr)
//
//        imageView = findViewById(R.id.imageView)
//        textResult = findViewById(R.id.textResult)
//        buttonSelect = findViewById(R.id.btnSelectImage)
//        buttonExtract = findViewById(R.id.btnExtractText)
//
//        detector = Interpreter(loadModelFile("easyocr-easyocrdetector.tflite"))
//        recognizer = Interpreter(loadModelFile("easyocr-easyocrrecognizer.tflite"))
//
//        buttonSelect.setOnClickListener {
//            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
//            startActivityForResult(intent, PICK_IMAGE_REQUEST)
//        }
//
//        buttonExtract.setOnClickListener {
//            selectedBitmap?.let { bitmap ->
//                launch {
//                    textResult.text = "텍스트 인식 중..."
//                    withContext(Dispatchers.Default) {
//                        processOCR(bitmap)
//                    }
//                }
//            } ?: run {
//                textResult.text = "먼저 이미지를 선택해주세요."
//            }
//        }
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
//            val imageUri: Uri? = data.data
//            if (imageUri != null) {
//                val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
//                val bitmap = BitmapFactory.decodeStream(inputStream)
//                imageView.setImageBitmap(bitmap)
//                selectedBitmap = bitmap
//                textResult.text = "이미지를 선택했습니다. 텍스트 추출 버튼을 눌러주세요."
//            }
//        }
//    }
//
//    private fun extractTextBoxesFromDetectorOutput(
//        output: Array<Array<Array<FloatArray>>>,
//        bitmap: Bitmap,
//        threshold: Float = 0.7f
//    ): List<Bitmap> {
//        val boxes = mutableListOf<Bitmap>()
//        val scoreMap = output[0]
//        val height = bitmap.height
//        val width = bitmap.width
//        val scaleY = height.toFloat() / 608f
//        val scaleX = width.toFloat() / 800f
//
//        for (y in scoreMap.indices) {
//            for (x in scoreMap[0].indices) {
//                val score = scoreMap[y][x][0]
//                if (score > threshold) {
//                    val boxLeft = (x * 4 * scaleX).toInt().coerceIn(0, width - 1)
//                    val boxTop = (y * 4 * scaleY).toInt().coerceIn(0, height - 1)
//                    val boxRight = (boxLeft + 100).coerceIn(0, width)
//                    val boxBottom = (boxTop + 64).coerceIn(0, height)
//                    val cropped = Bitmap.createBitmap(bitmap, boxLeft, boxTop, boxRight - boxLeft, boxBottom - boxTop)
//                    boxes.add(cropped)
//                }
//            }
//        }
//        return boxes
//    }
//
////    private fun processOCR(bitmap: Bitmap) {
////        // 1. Detector 실행
////        val detectorInput = preprocessDetectorInput(bitmap)
////        val detectorOutput = HashMap<Int, Any>()
////        val outputArray = Array(1) { Array(304) { Array(400) { FloatArray(2) } } }
////        detectorOutput[0] = outputArray
////
////        detector.runForMultipleInputsOutputs(arrayOf(detectorInput), detectorOutput)
////
////        // 2. Detector 출력으로부터 박스 추출
////        val croppedBoxes = extractTextBoxesFromDetectorOutput(outputArray, bitmap)
////        val results = mutableListOf<String>()
////
////        // 3. 박스마다 recognizer 실행
////        for (box in croppedBoxes) {
////            val recognizerInput = preprocessRecognizerInput(box)
////            val recognizerOutput = Array(1) { Array(249) { FloatArray(97) } }
////            recognizer.run(recognizerInput, recognizerOutput)
////
////            val text = decodeCTC(recognizerOutput, charset)
////            if (text.isNotBlank()) results.add(text)
////        }
////
////        // 4. 감지된 박스를 이미지에 시각화
////        val drawnBitmap = drawDetectionBoxes(bitmap, outputArray)
////
////        // 5. UI 업데이트 (메인 스레드에서)
////        runOnUiThread {
////            imageView.setImageBitmap(drawnBitmap)
////            textResult.text = if (results.isNotEmpty()) {
////                "인식된 텍스트: ${results.joinToString(" ")}"
////            } else {
////                "텍스트를 인식하지 못했습니다."
////            }
////        }
////    }
//private fun processOCR(bitmap: Bitmap) {
//    val recognizerInput = preprocessRecognizerInput(bitmap)
//    val recognizerOutput = Array(1) { Array(249) { FloatArray(97) } }
//    recognizer.run(recognizerInput, recognizerOutput)
//
//    val resultText = decodeCTC(recognizerOutput, charset)
//    runOnUiThread {
//        imageView.setImageBitmap(bitmap)
//        textResult.text = "인식된 텍스트: $resultText"
//    }
//}
//
//
//
//    private fun loadModelFile(modelName: String): MappedByteBuffer {
//        val fileDescriptor = assets.openFd(modelName)
//        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
//        val fileChannel = inputStream.channel
//        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
//    }
//
//    private fun preprocessDetectorInput(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
//        val resized = Bitmap.createScaledBitmap(bitmap, 800, 608, true)
//        val input = Array(1) { Array(3) { Array(608) { FloatArray(800) } } }
//
//        for (y in 0 until 608) {
//            for (x in 0 until 800) {
//                val pixel = resized.getPixel(x, y)
//                input[0][0][y][x] = (pixel shr 16 and 0xFF) / 255.0f
//                input[0][1][y][x] = (pixel shr 8 and 0xFF) / 255.0f
//                input[0][2][y][x] = (pixel and 0xFF) / 255.0f
//            }
//        }
//        return input
//    }
//
//    private fun preprocessRecognizerInput(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
//        val resized = Bitmap.createScaledBitmap(bitmap, 1000, 64, true)
//        val input = Array(1) { Array(1) { Array(64) { FloatArray(1000) } } }
//
//        for (y in 0 until 64) {
//            for (x in 0 until 1000) {
//                val pixel = resized.getPixel(x, y)
//                val gray = ((pixel shr 16 and 0xFF) + (pixel shr 8 and 0xFF) + (pixel and 0xFF)) / 3
//                input[0][0][y][x] = gray / 255.0f
//            }
//        }
//        return input
//    }
//
//    private fun decodeCTC(output: Array<Array<FloatArray>>, charset: List<Char>, blankIndex: Int = 0): String {
//        val sequence = mutableListOf<Int>()
//        val timeSteps = output[0]
//
//        var lastChar = -1
//        for (t in timeSteps.indices) {
//            val probs = timeSteps[t]
//            val maxIndex = probs.indices.maxByOrNull { probs[it] } ?: continue
//
//            if (maxIndex != blankIndex && maxIndex != lastChar) {
//                sequence.add(maxIndex)
//            }
//            lastChar = maxIndex
//        }
//
//        return sequence.mapNotNull { charset.getOrNull(it) }.joinToString("")
//    }
//
//    private fun drawDetectionBoxes(bitmap: Bitmap, outputArray: Array<Array<Array<FloatArray>>>): Bitmap {
//        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
//        val canvas = android.graphics.Canvas(mutableBitmap)
//        val paint = android.graphics.Paint().apply {
//            color = android.graphics.Color.RED
//            style = android.graphics.Paint.Style.STROKE
//            strokeWidth = 4f
//        }
//
//        val width = bitmap.width
//        val height = bitmap.height
//        val scaleX = width.toFloat() / 800f
//        val scaleY = height.toFloat() / 608f
//
//        val scoreMap = outputArray[0]
//        val threshold = 0.7f
//
//        for (y in scoreMap.indices) {
//            for (x in scoreMap[0].indices) {
//                val score = scoreMap[y][x][0]
//                if (score > threshold) {
//                    val boxLeft = (x * 4 * scaleX).toInt().coerceIn(0, width - 1)
//                    val boxTop = (y * 4 * scaleY).toInt().coerceIn(0, height - 1)
//                    val boxRight = (boxLeft + 200).coerceIn(0, width)
//                    val boxBottom = (boxTop + 80).coerceIn(0, height)
//                    canvas.drawRect(boxLeft.toFloat(), boxTop.toFloat(), boxRight.toFloat(), boxBottom.toFloat(), paint)
//                }
//            }
//        }
//
//        return mutableBitmap
//    }
//
//
//}
package com.example.tokkit

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.InputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class EasyOCRActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private lateinit var detector: Interpreter
    private lateinit var recognizer: Interpreter

    private lateinit var imageView: ImageView
    private lateinit var textResult: TextView
    private lateinit var buttonSelect: Button
    private lateinit var buttonExtract: Button

    private val PICK_IMAGE_REQUEST = 1001
    private var selectedBitmap: Bitmap? = null

    private val charset = listOf(
        '_',
        'A','B','C','D','E','F','G','H','I','J','K','L','M',
        'N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
        'a','b','c','d','e','f','g','h','i','j','k','l','m',
        'n','o','p','q','r','s','t','u','v','w','x','y','z'
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_easy_ocr)

        imageView = findViewById(R.id.imageView)
        textResult = findViewById(R.id.textResult)
        buttonSelect = findViewById(R.id.btnSelectImage)
        buttonExtract = findViewById(R.id.btnExtractText)

        detector = Interpreter(loadModelFile("easyocr-easyocrdetector.tflite"))
        recognizer = Interpreter(loadModelFile("easyocr-easyocrrecognizer.tflite"))

        buttonSelect.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        buttonExtract.setOnClickListener {
            selectedBitmap?.let { bitmap ->
                launch {
                    textResult.text = "텍스트 인식 중..."
                    withContext(Dispatchers.Default) {
                        processOCR(bitmap)
                    }
                }
            } ?: run {
                textResult.text = "먼저 이미지를 선택해주세요."
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri: Uri? = data.data
            if (imageUri != null) {
                val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                imageView.setImageBitmap(bitmap)
                selectedBitmap = bitmap
                textResult.text = "이미지를 선택했습니다. 텍스트 추출 버튼을 눌러주세요."
            }
        }
    }

    private fun processOCR(bitmap: Bitmap) {
        val recognizerInput = preprocessRecognizerInput(bitmap)
        val recognizerOutput = Array(1) { Array(249) { FloatArray(97) } }
        recognizer.run(recognizerInput, recognizerOutput)
        Log.d("CTC Output", decodeCTC(recognizerOutput, charset))


        val resultText = decodeCTC(recognizerOutput, charset)
        runOnUiThread {
            imageView.setImageBitmap(bitmap)
            textResult.text = "인식된 텍스트: $resultText"
        }
    }

    private fun loadModelFile(modelName: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    private fun preprocessRecognizerInput(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        val targetWidth = 1000
        val targetHeight = 64

        val resized = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        val input = Array(1) { Array(1) { Array(targetHeight) { FloatArray(targetWidth) } } }

        val width = resized.width
        val height = resized.height

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = resized.getPixel(x, y)
                val gray = ((pixel shr 16 and 0xFF) + (pixel shr 8 and 0xFF) + (pixel and 0xFF)) / 3
                input[0][0][y][x] = gray / 255.0f
            }
        }

        return input
    }


    private fun decodeCTC(output: Array<Array<FloatArray>>, charset: List<Char>, blankIndex: Int = 0): String {
        val sequence = mutableListOf<Int>()
        val timeSteps = output[0]

        var lastChar = -1
        for (t in timeSteps.indices) {
            val probs = timeSteps[t]
            val maxIndex = probs.indices.maxByOrNull { probs[it] } ?: continue

            if (maxIndex != blankIndex && maxIndex != lastChar) {
                sequence.add(maxIndex)
            }
            lastChar = maxIndex
        }

        return sequence.mapNotNull { charset.getOrNull(it) }.joinToString("")
    }
}
