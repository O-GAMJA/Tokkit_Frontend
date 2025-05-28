package com.example.tokkit

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.example.tokkit.databinding.ActivityGeneratedResultBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.tokkit.util.CustomToastUtil

class GeneratedResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGeneratedResultBinding
    private var imageUrl: String? = null
    private var imageLoaded = false
    private val TAG = "GeneratedResultActivity"

    // S3 관련 변수
    private var preSignedUrl: String? = null
    private var imageKey: String? = null
    private var s3UploadSuccessful = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGeneratedResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tagList = intent.getStringArrayListExtra("selectedTags") ?: arrayListOf()
        val selectedPath = intent.getStringExtra("selectedPath")
        imageUrl = intent.getStringExtra("IMAGE_URL")
        val useDefaultImage = intent.getBooleanExtra("USE_DEFAULT_IMAGE", false)

        // S3 관련 데이터 가져오기
        preSignedUrl = intent.getStringExtra("PRE_SIGNED_URL")
        imageKey = intent.getStringExtra("IMAGE_KEY")

        // 원본 데이터 가져오기
        val markdownContent = intent.getStringExtra("MARKDOWN_CONTENT") ?: ""
        val conversationText = intent.getStringExtra("CONVERSATION_TEXT") ?: ""
        val noteTitle = intent.getStringExtra("NOTE_TITLE")

        // 로그 추가
        Log.d(TAG, "GeneratedResultActivity에서 받은 데이터 - 마크다운: ${markdownContent.take(50)}...")
        Log.d(TAG, "GeneratedResultActivity에서 받은 데이터 - 대화: ${conversationText.take(50)}...")
        Log.d(TAG, "GeneratedResultActivity에서 받은 데이터 - 제목: $noteTitle")
        Log.d(TAG, "GeneratedResultActivity에서 받은 데이터 - S3 관련: preSignedUrl=${preSignedUrl?.take(30)}..., imageKey=$imageKey")
        val options = RequestOptions()
            .timeout(30000)//타임아웃 30초

        // 이미지 로드
        if (!imageUrl.isNullOrEmpty()) {
            // 서버에서 받은 이미지 URL 사용
            Glide.with(this)
                .load(imageUrl)
                .apply(options)
                .apply(RequestOptions().centerCrop())
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e(TAG, "이미지 로드 실패", e)
                        imageLoaded = false
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d(TAG, "이미지 로드 성공")
                        imageLoaded = true

                        // S3에 이미지 업로드 시도
                        if (!preSignedUrl.isNullOrEmpty()) {
                            uploadImageToS3((resource as? BitmapDrawable)?.bitmap)
                        }

                        return false
                    }
                })
                .into(binding.generatedImage)
        } else if (useDefaultImage) {
            // 기본 이미지 표시 (태그에 따라 다른 이미지 선택)
            val defaultImageRes = when {
                tagList.contains("JAVA") ||
                        tagList.contains("TCP/IP") || tagList.contains("데이터 통신") -> R.drawable.ic_tcp_ip
                else -> R.drawable.ic_default_image
            }

            Glide.with(this)
                .load(defaultImageRes)
                .apply(RequestOptions().centerCrop())
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e(TAG, "기본 이미지 로드 실패", e)
                        imageLoaded = false
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d(TAG, "기본 이미지 로드 성공")
                        imageLoaded = true
                        return false
                    }
                })
                .into(binding.generatedImage)
        }

        // 저장 버튼
        binding.saveText.setOnClickListener {
            if (imageLoaded) {
                if (s3UploadSuccessful && !imageKey.isNullOrEmpty()) {
                    // S3 업로드 성공시 imageKey만 전달
                    Log.d(TAG, "S3 업로드 성공, imageKey 전달: $imageKey")
                    saveWithS3ImageKey(tagList, selectedPath, markdownContent, conversationText, noteTitle, imageKey!!)
                } else {
                    // S3 업로드 실패 또는 시도하지 않은 경우 원래 방식으로 저장
                    Log.d(TAG, "로컬 이미지 저장 방식 사용")
                    saveGeneratedImage(tagList, selectedPath, markdownContent, conversationText, noteTitle)
                }
            } else {
                Log.e(TAG, "이미지가 로드되지 않아 저장할 수 없습니다")
                // 사용자에게 토스트 메시지 표시
                CustomToastUtil.showToast(
                    context = this,
                    message = "이미지가 아직 로드되지 않았습니다. 잠시 후 다시 시도해주세요.",
                    iconResId = R.drawable.ic_bot
                )
            }
        }

        // 다시 생성하기 버튼
        binding.regenerateButton.setOnClickListener {
            val markdownContent = intent.getStringExtra("MARKDOWN_CONTENT") ?: ""
            val conversationText = intent.getStringExtra("CONVERSATION_TEXT") ?: ""
            var noteTitle = intent.getStringExtra("NOTE_TITLE") ?: ""

            // 로그 추가 - 전달할 데이터 검증
            Log.d(TAG, "재생성 시작 - 전달할 데이터:")
            Log.d(TAG, "제목: $noteTitle")
            Log.d(TAG, "재생성: LoadingActivity로 제목 전달: $noteTitle")

            Log.d(TAG, "마크다운 내용 길이: ${markdownContent.length}")
            Log.d(TAG, "대화 내용 길이: ${conversationText.length}")

            // 데이터가 비어있는지 확인
            if (markdownContent.isBlank()) {
                Log.e(TAG, "마크다운 내용이 비어있어 재생성할 수 없습니다")
                CustomToastUtil.showToast(
                    context = this,
                    message = "재생성할 노트 내용이 없습니다",
                    iconResId = R.drawable.ic_bot
                )
                return@setOnClickListener
            }

            if (noteTitle.isBlank()) {
                Log.e(TAG, "노트 제목이 비어있어 재생성할 수 없습니다")
                // 제목이 비어있으면 기본값 설정
                noteTitle = "새 노트"
            }

            val intent = Intent(this, LoadingActivity::class.java)
            intent.putStringArrayListExtra("selectedTags", tagList)
            intent.putExtra("selectedPath", selectedPath)
            intent.putExtra("REGENERATE", true)

            // 원본 데이터 전달 - 확실히 전달되도록 확인
            intent.putExtra("NOTE_CONTENT", markdownContent) // 이미지 생성에 사용되는 내용
            intent.putExtra("MARKDOWN_CONTENT", markdownContent)
            intent.putExtra("CONVERSATION_TEXT", conversationText)
            intent.putExtra("NOTE_TITLE", noteTitle)

            Log.d(TAG, "LoadingActivity로 데이터 전달 - 마크다운: ${markdownContent.take(50)}...")
            Log.d(TAG, "LoadingActivity로 데이터 전달 - 대화: ${conversationText.take(50)}...")
            Log.d(TAG, "LoadingActivity로 데이터 전달 - 제목: $noteTitle")

            startActivity(intent)
            finish()
        }
    }

    private fun uploadImageToS3(bitmap: Bitmap?) {
        if (bitmap == null || preSignedUrl.isNullOrEmpty()) {
            Log.e(TAG, "S3 업로드 실패: 비트맵 또는 프리사인드 URL 없음")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "S3 업로드 시작")

                // 비트맵을 JPEG 바이트 배열로 변환
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                val imageBytes = outputStream.toByteArray()

                // OkHttp를 사용하여 S3에 업로드
                val client = OkHttpClient.Builder()
                    .build()

                val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaType())

                val request = Request.Builder()
                    .url(preSignedUrl!!)
                    .put(requestBody)
                    .build()

                val response = client.newCall(request).execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Log.d(TAG, "S3 업로드 성공: ${response.code}")
                        s3UploadSuccessful = true

                    } else {
                        Log.e(TAG, "S3 업로드 실패: ${response.code} - ${response.message}")

                        // 실패 메시지 표시
                        CustomToastUtil.showToast(
                            context = this@GeneratedResultActivity,
                            message = "이미지 업로드에 실패했습니다. 기본 저장 방식을 사용합니다.",
                            iconResId = R.drawable.ic_bot
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "S3 업로드 중 예외 발생", e)

                withContext(Dispatchers.Main) {
                    CustomToastUtil.showToast(
                        context = this@GeneratedResultActivity,
                        message = "이미지 업로드 중 오류 발생: ${e.message}",
                        iconResId = R.drawable.ic_bot
                    )
                }
            }
        }
    }

    private fun saveWithS3ImageKey(
        tagList: ArrayList<String>,
        selectedPath: String?,
        markdownContent: String,
        conversationText: String,
        noteTitle: String?,
        imageKey: String
    ) {
        // 노트 상세 화면으로 이동
        val intent = Intent(this, NoteDetailsActivity::class.java)

        // 이미지 식별자 전달 (우선순위 1)
        intent.putExtra("S3_IMAGE_KEY", imageKey)

        // 원본 이미지 URL도 백업으로 전달 (우선순위 3)
        if (!imageUrl.isNullOrEmpty()) {
            intent.putExtra("IMAGE_URL", imageUrl)
        }

        // 태그 및 경로 정보
        intent.putStringArrayListExtra("selectedTags", tagList)
        intent.putExtra("selectedPath", selectedPath)

        // 원본 데이터 추가
        intent.putExtra("MARKDOWN_CONTENT", markdownContent)
        intent.putExtra("CONVERSATION_TEXT", conversationText)
        intent.putExtra("NOTE_TITLE", noteTitle)

        Log.d(TAG, "NoteDetailsActivity로 S3 이미지 키 전달: $imageKey")
        Log.d(TAG, "NoteDetailsActivity로 원본 이미지 URL 전달: $imageUrl")

        startActivity(intent)
        finish()
    }

    private fun saveGeneratedImage(
        tagList: ArrayList<String>,
        selectedPath: String?,
        markdownContent: String,
        conversationText: String,
        noteTitle: String?
    ) {
        try {
            // 파일에 이미지 저장 (대용량 이미지를 Intent로 전달하지 않기 위함)
            val drawable = binding.generatedImage.drawable
            if (drawable !is BitmapDrawable) {
                Log.e(TAG, "drawable이 BitmapDrawable이 아님")

                // 원본 이미지 URL만 전달
                val intent = Intent(this, NoteDetailsActivity::class.java)
                intent.putExtra("IMAGE_URL", imageUrl)
                intent.putStringArrayListExtra("selectedTags", tagList)
                intent.putExtra("selectedPath", selectedPath)
                intent.putExtra("MARKDOWN_CONTENT", markdownContent)
                intent.putExtra("CONVERSATION_TEXT", conversationText)
                intent.putExtra("NOTE_TITLE", noteTitle)

                startActivity(intent)
                finish()
                return
            }

            val bitmap = drawable.bitmap

            // 비트맵 크기 로그
            Log.d(TAG, "비트맵 크기: ${bitmap.width}x${bitmap.height}, 바이트 수: ${bitmap.byteCount}")

            // 이미지 파일 저장 경로
            val imageFile = File(cacheDir, "generated_image_${System.currentTimeMillis()}.jpg")

            try {
                FileOutputStream(imageFile).use { out ->
                    // 압축 품질을 낮춰서 크기 줄이기 (90은 상당히 좋은 품질)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    out.flush()
                }

                Log.d(TAG, "이미지 파일 저장 완료: ${imageFile.absolutePath}, 크기: ${imageFile.length()} 바이트")

                // Intent로 여러 방식의 이미지 참조를 전달 (우선순위에 따라 사용)
                val intent = Intent(this, NoteDetailsActivity::class.java)

                // 우선순위 2: 파일 경로
                intent.putExtra("IMAGE_FILE_PATH", imageFile.absolutePath)

                // 우선순위 3: 원본 URL
                if (!imageUrl.isNullOrEmpty()) {
                    intent.putExtra("IMAGE_URL", imageUrl)
                }

                intent.putStringArrayListExtra("selectedTags", tagList)
                intent.putExtra("selectedPath", selectedPath)

                // 원본 데이터 추가
                intent.putExtra("MARKDOWN_CONTENT", markdownContent)
                intent.putExtra("CONVERSATION_TEXT", conversationText)
                intent.putExtra("NOTE_TITLE", noteTitle)

                Log.d(TAG, "NoteDetailsActivity로 이미지 파일 경로 전달: ${imageFile.absolutePath}")
                Log.d(TAG, "NoteDetailsActivity로 원본 이미지 URL 전달: $imageUrl")

                startActivity(intent)
                finish()

            } catch (e: Exception) {
                Log.e(TAG, "이미지 파일 저장 실패", e)

                // 파일 저장 실패 시 URL만 전달
                val intent = Intent(this, NoteDetailsActivity::class.java)
                intent.putExtra("IMAGE_URL", imageUrl)
                intent.putStringArrayListExtra("selectedTags", tagList)
                intent.putExtra("selectedPath", selectedPath)
                intent.putExtra("MARKDOWN_CONTENT", markdownContent)
                intent.putExtra("CONVERSATION_TEXT", conversationText)
                intent.putExtra("NOTE_TITLE", noteTitle)

                startActivity(intent)
                finish()
            }

        } catch (e: Exception) {
            Log.e(TAG, "이미지 저장 과정에서 예외 발생", e)

            // 오류 발생 시 URL만 전달
            val intent = Intent(this, NoteDetailsActivity::class.java)
            intent.putExtra("IMAGE_URL", imageUrl)
            intent.putStringArrayListExtra("selectedTags", tagList)
            intent.putExtra("selectedPath", selectedPath)
            intent.putExtra("MARKDOWN_CONTENT", markdownContent)
            intent.putExtra("CONVERSATION_TEXT", conversationText)
            intent.putExtra("NOTE_TITLE", noteTitle)

            startActivity(intent)
            finish()
        }
    }
}