package com.example.tokkit

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.example.tokkit.databinding.ActivityGeneratedResultBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class GeneratedResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGeneratedResultBinding
    private var imageUrl: String? = null
    private var imageLoaded = false
    private val TAG = "GeneratedResultActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGeneratedResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tagList = intent.getStringArrayListExtra("selectedTags") ?: arrayListOf()
        val selectedPath = intent.getStringExtra("selectedPath")
        imageUrl = intent.getStringExtra("IMAGE_URL")
        val useDefaultImage = intent.getBooleanExtra("USE_DEFAULT_IMAGE", false)

        // 원본 데이터 가져오기
        val markdownContent = intent.getStringExtra("MARKDOWN_CONTENT") ?: ""
        val conversationText = intent.getStringExtra("CONVERSATION_TEXT") ?: ""
        val noteTitle = intent.getStringExtra("NOTE_TITLE")

        // 로그 추가
        Log.d(TAG, "GeneratedResultActivity에서 받은 데이터 - 마크다운: ${markdownContent.take(50)}...")
        Log.d(TAG, "GeneratedResultActivity에서 받은 데이터 - 대화: ${conversationText.take(50)}...")
        Log.d(TAG, "GeneratedResultActivity에서 받은 데이터 - 제목: $noteTitle")

        // 이미지 로드
        if (!imageUrl.isNullOrEmpty()) {
            // 서버에서 받은 이미지 URL 사용
            Glide.with(this)
                .load(imageUrl)
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
                saveGeneratedImage(tagList, selectedPath, markdownContent, conversationText, noteTitle)
            } else {
                Log.e(TAG, "이미지가 로드되지 않아 저장할 수 없습니다")
                // 사용자에게 토스트 메시지 표시
                android.widget.Toast.makeText(
                    this,
                    "이미지가 아직 로드되지 않았습니다. 잠시 후 다시 시도해주세요.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }

        // 다시 생성하기 버튼
        binding.regenerateButton.setOnClickListener {
            val intent = Intent(this, LoadingActivity::class.java)
            intent.putStringArrayListExtra("selectedTags", tagList)
            intent.putExtra("selectedPath", selectedPath)
            intent.putExtra("REGENERATE", true)

            // 원본 데이터도 함께 전달
            intent.putExtra("MARKDOWN_CONTENT", markdownContent)
            intent.putExtra("CONVERSATION_TEXT", conversationText)
            intent.putExtra("NOTE_TITLE", noteTitle)

            startActivity(intent)
            finish()
        }
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

                // Intent로 이미지 파일 경로만 전달 + 원본 데이터도 함께 전달
                val intent = Intent(this, NoteDetailsActivity::class.java)
                intent.putExtra("IMAGE_FILE_PATH", imageFile.absolutePath)
                intent.putExtra("IMAGE_URL", imageUrl)
                intent.putStringArrayListExtra("selectedTags", tagList)
                intent.putExtra("selectedPath", selectedPath)
                // 원본 데이터 추가
                intent.putExtra("MARKDOWN_CONTENT", markdownContent)
                intent.putExtra("CONVERSATION_TEXT", conversationText)
                intent.putExtra("NOTE_TITLE", noteTitle)

                Log.d(TAG, "NoteDetailsActivity로 데이터 전달 - 마크다운: ${markdownContent.take(50)}...")
                Log.d(TAG, "NoteDetailsActivity로 데이터 전달 - 대화: ${conversationText.take(50)}...")
                Log.d(TAG, "NoteDetailsActivity로 데이터 전달 - 제목: $noteTitle")

                startActivity(intent)
                finish()

            } catch (e: Exception) {
                Log.e(TAG, "이미지 파일 저장 실패", e)
                android.widget.Toast.makeText(
                    this,
                    "이미지 저장에 실패했습니다: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }

        } catch (e: Exception) {
            Log.e(TAG, "이미지 저장 과정에서 예외 발생", e)
            android.widget.Toast.makeText(
                this,
                "이미지 처리 중 오류가 발생했습니다: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}