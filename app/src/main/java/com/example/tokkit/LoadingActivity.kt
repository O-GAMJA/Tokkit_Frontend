package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tokkit.databinding.ActivityLoadingBinding
import com.example.tokkit.data.remote.model.ImageGenerationRequest
import com.example.tokkit.data.remote.model.S3UrlResponse
import com.example.tokkit.util.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.File
import java.util.UUID

class LoadingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoadingBinding
    private val TAG = "LoadingActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tagList = intent.getStringArrayListExtra("selectedTags") ?: arrayListOf()
        val selectedPath = intent.getStringExtra("selectedPath")
        val noteContent = intent.getStringExtra("NOTE_CONTENT") ?: ""
        val markdownContent = intent.getStringExtra("MARKDOWN_CONTENT") ?: ""
        val conversationText = intent.getStringExtra("CONVERSATION_TEXT") ?: ""
        val noteTitle = intent.getStringExtra("NOTE_TITLE")
        val isRegenerate = intent.getBooleanExtra("REGENERATE", false) // 재생성 플래그 읽기

        // 로그 추가
        Log.d(TAG, "LoadingActivity에서 받은 데이터 - 마크다운: ${markdownContent.take(50)}...")
        Log.d(TAG, "LoadingActivity에서 받은 데이터 - 대화: ${conversationText.take(50)}...")
        Log.d(TAG, "LoadingActivity에서 받은 데이터 - 제목: $noteTitle")
        Log.d(TAG, "재생성 모드: $isRegenerate")

        // 애니메이션 시작
        binding.lottieAnimationView.playAnimation()

        // 태그 기반 스타일 생성
        val style = generateStyleFromTags(tagList)

        // 재생성 시 다른 스타일 사용
        val finalStyle = if (isRegenerate) {
            "$style, different style"  // 재생성 시 다른 결과가 나오도록 스타일 변경
        } else {
            style
        }

        Log.d(TAG, "사용할 스타일: $finalStyle")

        // API 호출
        generateImage(noteContent, finalStyle, tagList, selectedPath, markdownContent, conversationText, noteTitle)
    }

    private fun generateStyleFromTags(tags: ArrayList<String>): String {
        // 태그를 스타일로 변환 (예: 학습 관련 태그면 "educational, diagram", 자연 관련이면 "natural, photograph" 등)
        val styleMap = mapOf(
            "JAVA" to "computer code, programming",
            "TCP/IP" to "network diagram, technical",
            "데이터 통신" to "digital connection, technology",
            "Android" to "mobile interface, app design",
            "Kotlin" to "clean code, programming"
        )

        val styles = tags.mapNotNull { styleMap[it] }

        // 기본 스타일에 태그 기반 스타일 추가
        return if (styles.isNotEmpty()) {
            "${styles.joinToString(", ")}, digital art, minimalist"
        } else {
            "minimalist, digital art, illustration"
        }
    }

    private fun generateImage(
        noteContent: String,
        style: String,
        tagList: ArrayList<String>,
        selectedPath: String?,
        markdownContent: String,
        conversationText: String,
        noteTitle: String?
    ) {
        // 프롬프트에 제목 추가
        val titleContent = if (!noteTitle.isNullOrBlank()) {
            "Title: $noteTitle\n"
        } else {
            ""
        }

        // 최종 프롬프트(제목 + 내용)
        val finalContent = titleContent + noteContent.take(1000) // 너무 길지 않게 제한

        Log.d(TAG, "이미지 생성에 사용할 최종 프롬프트:")
        Log.d(TAG, "제목: $noteTitle")
        Log.d(TAG, "내용 샘플: ${noteContent.take(100)}...")
        Log.d(TAG, "스타일: $style")

        // 프로그레스 텍스트 업데이트
        binding.loadingText.text = "노트에 어울리는\n사진을 생성하고 있어요"

        lifecycleScope.launch {
            var retryCount = 0
            val maxRetries = 3

            var lastException: Exception? = null

            while (retryCount < maxRetries) {
                try {
                    Log.d(TAG, "이미지 생성 시도 ${retryCount + 1}/$maxRetries")
                    binding.loadingText.text = "노트에 어울리는\n사진을 생성하고 있어요 (${retryCount + 1}/$maxRetries)"

                    // 이미지 생성 API 호출
                    val api = RetrofitClient.imageApi

                    // 재시도할 때마다 약간 다른 프롬프트 사용
                    val retryContent = if (retryCount > 0) {
                        "$finalContent (variation ${retryCount})"
                    } else {
                        finalContent
                    }

                    val retryStyle = if (retryCount > 0) {
                        "$style, variation ${retryCount}"
                    } else {
                        style
                    }

                    val request = ImageGenerationRequest(retryContent, retryStyle)

                    Log.d(TAG, "이미지 생성 API 호출 시작 (시도 ${retryCount + 1}/$maxRetries)")

                    val response = api.generateImage(request)

                    Log.d(TAG, "이미지 생성 API 응답: $response")

                    if (response.isSuccess) {
                        // 이미지 생성 성공 - 이미지 URL 획득
                        val imageUrl = response.result.imageUrl

                        if (imageUrl.isNullOrEmpty()) {
                            Log.e(TAG, "API 응답은 성공했지만 이미지 URL이 비어있음")
                            retryCount++
                            delay(2000)
                            continue
                        }

                        try {
                            // S3 프리사인드 URL 발급 요청
                            val s3Api = RetrofitClient.s3Api
                            val uniqueFileName = "note_image_${UUID.randomUUID()}.jpg"

                            Log.d(TAG, "S3 프리사인드 URL 요청 시작: fileType=profile, fileName=$uniqueFileName")

                            val s3Response = s3Api.getPreSignedUrl("profile", uniqueFileName)

                            if (s3Response.isSuccess) {
                                // S3 URL 발급 성공
                                val preSignedUrl = s3Response.result.preSignedUrl
                                val imageKey = s3Response.result.imageKey

                                Log.d(TAG, "S3 프리사인드 URL 발급 성공: imageKey=$imageKey")

                                // 결과 화면으로 이동
                                val intent = Intent(this@LoadingActivity, GeneratedResultActivity::class.java)
                                intent.putExtra("IMAGE_URL", imageUrl)
                                intent.putExtra("PRE_SIGNED_URL", preSignedUrl)
                                intent.putExtra("IMAGE_KEY", imageKey)
                                intent.putStringArrayListExtra("selectedTags", tagList)
                                intent.putExtra("selectedPath", selectedPath)
                                intent.putExtra("MARKDOWN_CONTENT", markdownContent)
                                intent.putExtra("CONVERSATION_TEXT", conversationText)
                                intent.putExtra("NOTE_TITLE", noteTitle)

                                Log.d(TAG, "GeneratedResultActivity로 제목 전달: $noteTitle")
                                Log.d(TAG, "GeneratedResultActivity로 데이터 전달 완료")

                                startActivity(intent)
                                finish()
                                return@launch
                            } else {
                                // S3 URL 발급 실패 - 기존 방식으로 폴백
                                Log.e(TAG, "S3 프리사인드 URL 발급 실패: ${s3Response.message}")

                                // 기존 방식으로 이미지 URL만 전달
                                val intent = Intent(this@LoadingActivity, GeneratedResultActivity::class.java)
                                intent.putExtra("IMAGE_URL", imageUrl)
                                intent.putStringArrayListExtra("selectedTags", tagList)
                                intent.putExtra("selectedPath", selectedPath)
                                intent.putExtra("MARKDOWN_CONTENT", markdownContent)
                                intent.putExtra("CONVERSATION_TEXT", conversationText)
                                intent.putExtra("NOTE_TITLE", noteTitle)

                                startActivity(intent)
                                finish()
                                return@launch
                            }
                        } catch (e: Exception) {
                            // S3 URL 요청 실패 - 기존 방식으로 폴백
                            Log.e(TAG, "S3 URL 요청 중 예외 발생", e)
                            lastException = e

                            val intent = Intent(this@LoadingActivity, GeneratedResultActivity::class.java)
                            intent.putExtra("IMAGE_URL", imageUrl)
                            intent.putStringArrayListExtra("selectedTags", tagList)
                            intent.putExtra("selectedPath", selectedPath)
                            intent.putExtra("MARKDOWN_CONTENT", markdownContent)
                            intent.putExtra("CONVERSATION_TEXT", conversationText)
                            intent.putExtra("NOTE_TITLE", noteTitle)

                            startActivity(intent)
                            finish()
                            return@launch
                        }
                    } else {
                        // 이미지 생성 실패
                        Log.e(TAG, "이미지 생성 실패: ${response.message}")
                        lastException = Exception("API 응답 실패: ${response.message}")
                        retryCount++

                        if (retryCount >= maxRetries) {
                            fallbackToDefaultImage(tagList, selectedPath, markdownContent, conversationText, noteTitle)
                            return@launch
                        }

                        // 재시도 메시지 표시
                        binding.loadingText.text = "연결 재시도 중...\n(${retryCount}/${maxRetries})"
                        delay(2000) // 2초 대기 후 재시도
                    }
                } catch (e: HttpException) {
                    Log.e(TAG, "HTTP 오류 (시도 ${retryCount + 1}/$maxRetries): ${e.code()}", e)
                    lastException = e
                    retryCount++

                    if (retryCount >= maxRetries) {
                        fallbackToDefaultImage(tagList, selectedPath, markdownContent, conversationText, noteTitle)
                        return@launch
                    }

                    binding.loadingText.text = "연결 재시도 중...\n(${retryCount}/${maxRetries})"
                    delay(2000)
                } catch (e: Exception) {
                    Log.e(TAG, "오류 발생 (시도 ${retryCount + 1}/$maxRetries): ${e.message}", e)
                    lastException = e
                    retryCount++

                    if (retryCount >= maxRetries) {
                        fallbackToDefaultImage(tagList, selectedPath, markdownContent, conversationText, noteTitle)
                        return@launch
                    }

                    binding.loadingText.text = "연결 재시도 중...\n(${retryCount}/${maxRetries})"
                    delay(2000)
                }
            }

            // 모든 재시도가 실패한 경우
            Log.e(TAG, "모든 재시도 실패", lastException)
            fallbackToDefaultImage(tagList, selectedPath, markdownContent, conversationText, noteTitle)
        }
    }

    private fun fallbackToDefaultImage(
        tagList: ArrayList<String>,
        selectedPath: String?,
        markdownContent: String,
        conversationText: String,
        noteTitle: String?
    ) {
        // 실패 메시지 표시
        binding.loadingText.text = "이미지 생성에 실패했습니다.\n기본 이미지를 사용합니다."

        // 잠시 대기 후 다음 화면으로 이동 (사용자가 메시지를 볼 수 있도록)
        lifecycleScope.launch {
            delay(1500)

            // API 실패 시 기본 이미지 사용
            val intent = Intent(this@LoadingActivity, GeneratedResultActivity::class.java)
            intent.putStringArrayListExtra("selectedTags", tagList)
            intent.putExtra("selectedPath", selectedPath)
            intent.putExtra("USE_DEFAULT_IMAGE", true)

            // 노트 내용에 따라 기본 이미지 선택
            when {
                markdownContent.contains("operating system", ignoreCase = true) ->
                    intent.putExtra("DEFAULT_IMAGE_TYPE", "OS")
                markdownContent.contains("network", ignoreCase = true) ->
                    intent.putExtra("DEFAULT_IMAGE_TYPE", "NETWORK")
            }

            // 원본 데이터 전달
            intent.putExtra("MARKDOWN_CONTENT", markdownContent)
            intent.putExtra("CONVERSATION_TEXT", conversationText)
            intent.putExtra("NOTE_TITLE", noteTitle)

            Log.d(TAG, "fallback - GeneratedResultActivity로 데이터 전달 - 마크다운: ${markdownContent.take(50)}...")
            Log.d(TAG, "fallback - GeneratedResultActivity로 데이터 전달 - 대화: ${conversationText.take(50)}...")
            Log.d(TAG, "fallback - GeneratedResultActivity로 데이터 전달 - 제목: $noteTitle")

            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 애니메이션 정리
        binding.lottieAnimationView.cancelAnimation()
    }
}