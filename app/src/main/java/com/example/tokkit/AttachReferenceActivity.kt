package com.example.tokkit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.tokkit.data.remote.api.OcrApiService
import com.example.tokkit.databinding.ActivityAttachReferenceBinding
import com.example.tokkit.util.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import com.example.tokkit.util.CustomToastUtil

class AttachReferenceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAttachReferenceBinding
    private val TAG = "OCR_API"
    private var ocrResultText = ""


    // 저장소 접근 요청 코드
    private val STORAGE_PERMISSION_REQUEST_CODE = 100

    // 현재 선택 모드 (사진 또는 파일)
    private var currentMode = "NONE"

    // 사진 및 파일 선택 결과 처리
    private val getImageContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadImageForOcr(uri)
        } else {
            Log.d(TAG, "사용자가 이미지 선택을 취소했습니다")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttachReferenceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //  뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 사진 첨부 버튼
        binding.btnAttachPhoto.setOnClickListener {
            currentMode = "PHOTO"
            checkPermissionAndProceed()
        }

        // 파일 첨부 버튼
        binding.btnAttachFile.setOnClickListener {
            currentMode = "FILE"
            checkPermissionAndProceed()
        }

        // 건너뛰기 버튼
        binding.btnSkip.setOnClickListener {
            navigateToChatScreen()
        }
    }

    private fun checkPermissionAndProceed() {
        val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, requiredPermission) == PackageManager.PERMISSION_GRANTED -> {
                // 이미 권한이 있는 경우
                proceedWithImageSelection()
            }
            shouldShowRequestPermissionRationale(requiredPermission) -> {
                // 권한 요청이 거부된 적이 있는 경우, 설명 다이얼로그 표시
                showPermissionExplanationDialog(requiredPermission)
            }
            else -> {
                // 처음 권한 요청하는 경우
                requestPermission(requiredPermission)
            }
        }
    }

    private fun requestPermission(permission: String) {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(permission),
            STORAGE_PERMISSION_REQUEST_CODE
        )
    }

    private fun showPermissionExplanationDialog(permission: String) {
        AlertDialog.Builder(this)
            .setTitle("권한 필요")
            .setMessage("이미지를 처리하기 위해 저장소 접근 권한이 필요합니다. 권한을 허용해주세요.")
            .setPositiveButton("권한 요청") { _, _ ->
                requestPermission(permission)
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
                CustomToastUtil.showToast(
                    context = this,
                    message = "권한이 거부되어 기능을 사용할 수 없습니다.",
                    iconResId = R.drawable.ic_bot
                )
            }
            .create()
            .show()
    }

    private fun showAppSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("권한 필요")
            .setMessage("이 기능을 사용하려면 저장소 접근 권한이 필요합니다. 앱 설정에서 권한을 허용해주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
                CustomToastUtil.showToast(
                    context = this,
                    message = "권한이 거부되어 기능을 사용할 수 없습니다.",
                    iconResId = R.drawable.ic_bot
                )
            }
            .create()
            .show()
    }

    private fun proceedWithImageSelection() {
        when (currentMode) {
            "PHOTO", "FILE" -> {
                getImageContent.launch("image/*")
            }
        }
    }

    private fun uploadImageForOcr(uri: Uri) {
        // 로딩 메시지 표시
        CustomToastUtil.showToast(
            context = this,
            message = "이미지 처리 중...",
            iconResId = R.drawable.ic_bot
        )
        Log.d(TAG, "이미지 업로드 시작: $uri")

        try {
            // URI를 File로 변환
            val file = uriToFile(uri)

            if (file != null) {
                // Multipart 요청 준비
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)

                Log.d(TAG, "파일 준비 완료: ${file.name}, 크기: ${file.length()} 바이트")

                // API 호출
                lifecycleScope.launch {
                    try {
                        Log.d(TAG, "OCR API 호출 시작")
                        val ocrApi = RetrofitClient.createService(OcrApiService::class.java)
                        val response = ocrApi.processImage(imagePart)

                        if (response.isSuccess) {
                            // API 응답 로그 출력
                            ocrResultText = response.result.translatedText

                            Log.d(TAG, "OCR 처리 결과: $ocrResultText")

                            // 채팅 화면으로 이동
                            navigateToChatScreen()
                        } else {
                            Log.e(TAG, "OCR API 응답 실패: ${response.message}")
                            CustomToastUtil.showToast(
                                context = this@AttachReferenceActivity,
                                message = "텍스트 추출 실패: ${response.message}",
                                iconResId = R.drawable.ic_bot
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "OCR API 호출 중 오류 발생", e)
                        CustomToastUtil.showToast(
                            context = this@AttachReferenceActivity,
                            message = "이미지 처리 중 오류가 발생했습니다.",
                            iconResId = R.drawable.ic_bot
                        )
                    }
                }
            } else {
                Log.e(TAG, "파일 변환 실패")
                CustomToastUtil.showToast(
                    context = this,
                    message = "이미지 파일을 처리할 수 없습니다.",
                    iconResId = R.drawable.ic_bot
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "이미지 업로드 중 오류", e)
            CustomToastUtil.showToast(
                context = this,
                message = "이미지 처리 중 오류가 발생했습니다.",
                iconResId = R.drawable.ic_bot
            )
        }
    }

    private fun uriToFile(uri: Uri): File? {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = File(cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")

            FileOutputStream(file).use { outputStream ->
                inputStream.use { input ->
                    input.copyTo(outputStream)
                }
            }

            Log.d(TAG, "URI를 File로 변환 성공: ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            Log.e(TAG, "URI를 파일로 변환 중 오류", e)
            return null
        }
    }

    private fun navigateToChatScreen() {
        val intent = Intent(this, ChatActivity::class.java)

        // OCR 결과가 있는 경우에만 전달
        if (ocrResultText.isNotEmpty()) {
            intent.putExtra("OCR_TEXT", ocrResultText)
        }

        startActivity(intent)
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한 승인됨
                proceedWithImageSelection()
            } else {
                // 권한 거부됨
                if (!shouldShowRequestPermissionRationale(permissions[0])) {
                    // 다시 묻지 않음 선택한 경우 - 설정으로 이동하도록 안내
                    showAppSettingsDialog()
                } else {
                    // 일반 거부
                    CustomToastUtil.showToast(
                        context = this,
                        message = "이미지 처리를 위해 저장소 접근 권한이 필요합니다.",
                        iconResId = R.drawable.ic_bot
                    )
                }
            }
        }
    }
}