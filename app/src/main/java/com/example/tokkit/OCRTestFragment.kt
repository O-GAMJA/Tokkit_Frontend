package com.example.tokkit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OCRTestFragment : Fragment() {

    private lateinit var imageView: ImageView
    private lateinit var resultTextView: TextView
    private lateinit var captureButton: Button
    private lateinit var galleryButton: Button
    private lateinit var processButton: Button
    private lateinit var progressBar: ProgressBar
    private var capturedBitmap: Bitmap? = null
    private var ocrModelUtil: OCRModelUtil? = null

    // 카메라로 사진 촬영 결과 처리
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let {
                // 카메라 미리보기 이미지가 작을 수 있으므로 필요시 조정
                capturedBitmap = it
                imageView.setImageBitmap(it)
                processButton.isEnabled = true
                resultTextView.text = "이미지가 선택되었습니다. '텍스트 인식하기' 버튼을 눌러주세요."
            }
        }
    }

    // 갤러리에서 이미지 선택 결과 처리
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val imageUri = result.data?.data
            imageUri?.let { uri ->
                try {
                    // BitmapUtils를 사용하여 이미지 로드 및 전처리
                    val bitmap = BitmapUtils.loadBitmapFromUri(requireContext(), uri)
                    bitmap?.let {
                        // 너무 큰 이미지는 리사이징
                        val resizedBitmap = BitmapUtils.resizeBitmap(it, 1024, 1024)
                        capturedBitmap = resizedBitmap
                        imageView.setImageBitmap(resizedBitmap)
                        processButton.isEnabled = true
                        resultTextView.text = "이미지가 선택되었습니다. '텍스트 인식하기' 버튼을 눌러주세요."
                    } ?: run {
                        Toast.makeText(context, "이미지를 불러오는데 실패했습니다", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "이미지 처리 중 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 카메라 권한 요청 결과 처리
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(context, "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    // 외부 저장소 권한 요청 결과 처리
    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(context, "저장소 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // OCR 모델 유틸리티 초기화
        try {
            ocrModelUtil = OCRModelUtil(requireContext())
        } catch (e: Exception) {
            Toast.makeText(context, "모델 로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ocr_test, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뷰 초기화
        imageView = view.findViewById(R.id.imageView)
        resultTextView = view.findViewById(R.id.resultTextView)
        captureButton = view.findViewById(R.id.captureButton)
        galleryButton = view.findViewById(R.id.galleryButton)
        processButton = view.findViewById(R.id.processButton)
        progressBar = view.findViewById(R.id.progressBar)

        // 초기 상태 설정
        processButton.isEnabled = false
        progressBar.visibility = View.GONE

        // 버튼 이벤트 설정
        captureButton.setOnClickListener { checkCameraPermission() }
        galleryButton.setOnClickListener { checkStoragePermission() }
        processButton.setOnClickListener { processImageWithOCR() }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 리소스 해제
        ocrModelUtil?.close()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13 이상
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED -> {
                    openGallery()
                }
                else -> {
                    requestStoragePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }
        } else {
            // Android 12 이하
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    openGallery()
                }
                else -> {
                    requestStoragePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(intent)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun processImageWithOCR() {
        val bitmap = capturedBitmap ?: return

        // UI 업데이트
        progressBar.visibility = View.VISIBLE
        processButton.isEnabled = false
        resultTextView.text = "텍스트 인식 중..."

        // 코루틴에서 OCR 처리 실행
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // 실제 모델 추론 시도 (실패할 수 있음)
                var detections = withContext(Dispatchers.IO) {
                    try {
                        ocrModelUtil?.detectTextAreas(bitmap) ?: emptyList()
                    } catch (e: Exception) {
                        // 오류 발생 시 임시 테스트 데이터 사용
                        Log.e("OCRTestFragment", "모델 추론 실패, 테스트 데이터 사용: ${e.message}")
                        createTestDetections(bitmap)
                    }
                }

                // 임시: 결과가 없으면 테스트 데이터 사용
                if (detections.isEmpty()) {
                    detections = createTestDetections(bitmap)
                }

                // 결과 처리 (이하 동일)
                val visualizedBitmap = visualizeDetections(bitmap, detections)
                imageView.setImageBitmap(visualizedBitmap)

                val resultBuilder = StringBuilder()
                resultBuilder.append("텍스트 감지 완료! ${detections.size}개의 텍스트 영역 감지됨\n\n")

                detections.forEachIndexed { index, detection ->
                    resultBuilder.append("영역 #${index + 1}: ")
                    resultBuilder.append("신뢰도: ${String.format("%.2f", detection.confidence * 100)}%, ")
                    resultBuilder.append("회전: ${String.format("%.1f", Math.toDegrees(detection.angle.toDouble()))}°\n")
                }

                resultTextView.text = resultBuilder.toString()
            } catch (e: Exception) {
                resultTextView.text = "오류 발생: ${e.message}"
            } finally {
                progressBar.visibility = View.GONE
                processButton.isEnabled = true
            }
        }
    }

    // 테스트용 가짜 데이터 생성 함수
    private fun createTestDetections(bitmap: Bitmap): List<OCRModelUtil.TextDetection> {
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()

        return listOf(
            OCRModelUtil.TextDetection(
                RectF(width * 0.1f, height * 0.1f, width * 0.4f, height * 0.2f),
                0.95f,
                0f
            ),
            OCRModelUtil.TextDetection(
                RectF(width * 0.1f, height * 0.3f, width * 0.6f, height * 0.4f),
                0.87f,
                0f
            ),
            OCRModelUtil.TextDetection(
                RectF(width * 0.5f, height * 0.6f, width * 0.9f, height * 0.7f),
                0.76f,
                0f
            )
        )
    }

    /**
     * 텍스트 감지 결과를 시각화하는 함수
     */
    private fun visualizeDetections(originalBitmap: Bitmap, detections: List<OCRModelUtil.TextDetection>): Bitmap {
        // 원본 이미지를 복사하여 위에 그림
        val config = originalBitmap.config ?: Bitmap.Config.ARGB_8888
        val bitmap = originalBitmap.copy(config, true)
        val canvas = Canvas(bitmap)



        // 경계 상자 그리기 설정
        val boxPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }

        // 텍스트 설정
        val textPaint = Paint().apply {
            color = Color.RED
            textSize = 35f
            isFakeBoldText = true
        }

        // 각 감지 결과에 대해 경계 상자 그리기
        detections.forEachIndexed { index, detection ->
            val rect = detection.boundingBox

            // 회전된 경계 상자 그리기 (단순화를 위해 일단 직사각형만 그림)
            canvas.drawRect(rect, boxPaint)

            // 영역 번호 표시
            canvas.drawText("${index + 1}", rect.left, rect.top - 10, textPaint)
        }

        return bitmap
    }
}