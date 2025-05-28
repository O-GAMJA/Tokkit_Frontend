package com.example.tokkit

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.tokkit.data.remote.api.S3ApiService
import com.example.tokkit.data.remote.model.NoteCreateRequest
import com.example.tokkit.databinding.ActivityNoteDetailsBinding
import com.example.tokkit.genie.ConversationManager
import com.example.tokkit.util.RetrofitClient
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.example.tokkit.util.CustomToastUtil


class NoteDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNoteDetailsBinding

    private var currentTagList = mutableListOf<String>()
    private var selectedPath: String? = null
    private val REQUEST_GALLERY_IMAGE = 1001
    private var isPublic = true
    private var selectedDirectoryId: Int? = null

    // LocalDream에서 생성된 이미지 관련 변수
    private var generatedImageBitmap: Bitmap? = null
    private var generatedImagePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent에서 데이터 가져오기
        val markdownContent = intent.getStringExtra("MARKDOWN_CONTENT")
        val conversationText = intent.getStringExtra("CONVERSATION_TEXT")
        val noteTitle = intent.getStringExtra("NOTE_TITLE")
        val imageUrl = intent.getStringExtra("IMAGE_URL")
        val s3ImageKey = intent.getStringExtra("S3_IMAGE_KEY")

        // 로그로 데이터 확인
        Log.d("NoteDetails", "마크다운 내용: $markdownContent")
        Log.d("NoteDetails", "대화 내용: $conversationText")
        Log.d("NoteDetails", "노트 제목: $noteTitle")
        Log.d("NoteDetails", "이미지 URL: $imageUrl")
        Log.d("NoteDetails", "S3 이미지 키: $s3ImageKey")

        // ondevice stable diffusion에서 온 이미지 경로 확인
        val generatedImagePath = intent.getStringExtra("GENERATED_IMAGE_PATH")
        val generatedImageSuccess = intent.getBooleanExtra("GENERATED_IMAGE_SUCCESS", false)
        Log.d("NoteDetails", "생성된 이미지 경로: $generatedImagePath")
        Log.d("NoteDetails", "생성된 이미지 성공 여부: $generatedImageSuccess")

        // 공개 설정 라디오 버튼 리스너 설정
        binding.visibilityRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            isPublic = checkedId == R.id.publicOption
            Log.d("NoteDetails", "공개 설정 변경: is_public = $isPublic")
        }

        // 태그 초기화
        val tagList = intent.getStringArrayListExtra("selectedTags")
        if (!tagList.isNullOrEmpty()) {
            binding.tagContent.visibility = View.INVISIBLE
            binding.tagContainerInNote.visibility = View.VISIBLE
            renderSelectedTags(tagList)
            Log.d("NoteDetails", "초기 태그 목록: $tagList")
        } else {
            Log.d("NoteDetails", "초기 태그 목록: 비어있음")
        }

        // 경로 초기화
        selectedPath = intent.getStringExtra("selectedPath")
        if (!selectedPath.isNullOrEmpty()) {
            binding.storageDetail.text = selectedPath
            Log.d("NoteDetails", "저장 위치: $selectedPath")
        } else {
            Log.d("NoteDetails", "저장 위치: 지정되지 않음")
        }

        // 저장 버튼
        binding.saveNoteButton.setOnClickListener {
            // 현재 공개 설정, 태그, 저장 위치 정보 로그 출력
            Log.d("NoteDetails", "저장 시점 정보:")
            Log.d("NoteDetails", "- 공개 설정: is_public = $isPublic")
            Log.d("NoteDetails", "- 태그 목록: $currentTagList")
            Log.d("NoteDetails", "- 저장 위치: $selectedPath")
            Log.d("NoteDetails", "- 디렉토리 ID: $selectedDirectoryId")


            // API 호출 시작 로그 추가
            Log.d("NoteDetails", "노트 저장 API 호출 시작")

            // 저장 시도 메시지 표시
            CustomToastUtil.showToast(
                context = this,
                message = "노트를 저장 중입니다...",
                iconResId = R.drawable.ic_bot
            )

            saveNoteToServer(
                noteTitle = intent.getStringExtra("NOTE_TITLE") ?: "노트 제목",
                markdownContent = intent.getStringExtra("MARKDOWN_CONTENT") ?: "",
                conversationText = intent.getStringExtra("CONVERSATION_TEXT") ?: "",
                isPublic = isPublic,
                s3ImageKey = s3ImageKey,
                directoryId = selectedDirectoryId
            )
        }

        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 이미지 업로드 버튼 - 터치한 위치 기준으로 팝업 표시
        binding.imageUpload.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val x = event.rawX.toInt()
                val y = event.rawY.toInt()
                showImageChoicePopupAt(x, y)
            }
            true
        }

        // 태그 관리
        binding.tagForward.setOnClickListener {
            val intent = Intent(this, TagManageActivity::class.java)
            intent.putStringArrayListExtra("existingTags", ArrayList(currentTagList))
            startActivityForResult(intent, 101)
        }

        // 저장 위치 관리
        binding.storageForward.setOnClickListener {
            val intent = Intent(this, SaveLocationActivity::class.java)
            intent.putExtra("NOTE_TITLE", noteTitle) // 현재 제목을 인텐트에 넣기
            startActivityForResult(intent, 102)
        }

        // ondevice stable diffusion앱에서 직접 생성된 이미지 처리 (onCreate에서)
        if (generatedImageSuccess && !generatedImagePath.isNullOrEmpty()) {
            Log.d("NoteDetails", "onCreate에서 생성된 이미지 직접 처리: $generatedImagePath")
            loadGeneratedImageDirectly(generatedImagePath)
        } else {
            // 기존 이미지 표시 처리
            setupImageDisplay(s3ImageKey, imageUrl)
        }
    }

    // 생성된 이미지를 직접 로드하는 함수 추가
    private fun loadGeneratedImageDirectly(imagePath: String) {
        val imageFile = File(imagePath)
        if (imageFile.exists()) {
            Log.d("NoteDetails", "onCreate에서 생성된 이미지 로드: $imagePath")

            // 이미지 파일을 비트맵으로 로드하여 저장
            generatedImageBitmap = BitmapFactory.decodeFile(imagePath)
            generatedImagePath = imagePath

            Glide.with(this)
                .load(imageFile)
                .error(R.drawable.ic_default_image)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("NoteDetails", "onCreate에서 생성된 이미지 로드 실패: ${e?.message}", e)
                        // 기본 이미지 표시로 폴백
                        binding.imageUpload.setImageResource(R.drawable.image_upload)
                        applyImageLayoutSettings()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d("NoteDetails", "onCreate에서 생성된 이미지 로드 성공")

                        // 실제 이미지 모드로 레이아웃 설정
                        val layoutParams = binding.imageUpload.layoutParams as ConstraintLayout.LayoutParams
                        layoutParams.width = dpToPx(300)
                        layoutParams.height = dpToPx(300)
                        binding.imageUpload.scaleType = ImageView.ScaleType.CENTER_CROP
                        layoutParams.topMargin = dpToPx(0)
                        binding.imageUpload.layoutParams = layoutParams
                        binding.imageUpload.adjustViewBounds = true
                        binding.imageUpload.requestLayout()

                        // 성공 메시지 표시
                        //Toast.makeText(this@NoteDetailsActivity, "이미지가 생성되었습니다!", Toast.LENGTH_SHORT).show()

                        // 임시 파일 정리 (약간의 지연 후) - LocalDream의 임시 파일인 경우만
                        val isTempFile = intent.getBooleanExtra("IS_TEMP_FILE", false)
                        if (isTempFile) {
                            binding.imageUpload.postDelayed({
                                try {
                                    if (imageFile.exists()) {
                                        val deleted = imageFile.delete()
                                        Log.d("NoteDetails", "임시 파일 삭제 ${if (deleted) "성공" else "실패"}: ${imageFile.absolutePath}")
                                    }
                                } catch (e: Exception) {
                                    Log.w("NoteDetails", "임시 파일 삭제 실패", e)
                                }
                            }, 3000) // 3초 후 삭제
                        }

                        return false
                    }
                })
                .into(binding.imageUpload)

        } else {
            Log.e("NoteDetails", "onCreate에서 생성된 이미지 파일 찾을 수 없음: $imagePath")
            // 기본 이미지 표시로 폴백
            binding.imageUpload.setImageResource(R.drawable.image_upload)
            applyImageLayoutSettings()
        }
    }


    private fun setupImageDisplay(s3ImageKey: String?, imageUrl: String?) {
        Log.d("NoteDetails", "이미지 표시 시작 - S3 키: $s3ImageKey, URL: $imageUrl")

        // 이미지 업로드 상태에서 넘어왔는지 확인
        val showImageUploadButton = intent.getBooleanExtra("SHOW_IMAGE_UPLOAD_BUTTON", false)

        if (showImageUploadButton) {
            // 이미지 업로드 버튼 상태로 표시
            Log.d("NoteDetails", "이미지 업로드 버튼 표시")
            binding.imageUpload.setImageResource(R.drawable.image_upload)
            applyImageLayoutSettings()
            return
        }

        // 이미지 표시 우선순위:
        // 1. S3 이미지 키
        // 2. 이미지 파일 경로
        // 3. 이미지 URL
        // 4. 바이트 배열

        var imageLoaded = false

        // 1. S3 이미지 키가 있는 경우
        if (!s3ImageKey.isNullOrEmpty()) {
            Log.d("NoteDetails", "S3 이미지 키를 사용하여 이미지 표시 시도: $s3ImageKey")

            // S3 이미지 URL 구성
            val fullImageUrl = "http://52.79.86.14:8080/image/$s3ImageKey"

            Glide.with(this)
                .load(fullImageUrl)
                .error(R.drawable.ic_default_image) // 오류 시 기본 이미지 표시
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("NoteDetails", "S3 이미지 로드 실패: ${e?.message}", e)
                        // 다음 방법으로 시도
                        loadImageFromPath()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d("NoteDetails", "S3 이미지 로드 성공")
                        imageLoaded = true
                        applyImageLayoutSettings()
                        return false
                    }
                })
                .into(binding.imageUpload)

            if (imageLoaded) return
        }

        // 2. 이미지 파일 경로가 있는 경우
        loadImageFromPath()
    }

    private fun loadImageFromPath() {
        // 일반적인 이미지 파일 경로 확인
        var imageFilePath = intent.getStringExtra("IMAGE_FILE_PATH")

        // ondevice stable diffusion에서 생성된 이미지 경로도 확인
        if (imageFilePath.isNullOrEmpty()) {
            imageFilePath = intent.getStringExtra("GENERATED_IMAGE_PATH")
            Log.d("NoteDetails", "LocalDream 생성 이미지 경로 사용: $imageFilePath")
        }

        var imageLoaded = false

        if (!imageFilePath.isNullOrEmpty()) {
            val imageFile = File(imageFilePath)
            if (imageFile.exists()) {
                Log.d("NoteDetails", "로컬 이미지 파일 표시 시도: $imageFilePath")

                // 이미지 파일을 비트맵으로 로드하여 저장
                generatedImageBitmap = BitmapFactory.decodeFile(imageFilePath)
                generatedImagePath = imageFilePath

                Glide.with(this)
                    .load(imageFile)
                    .error(R.drawable.ic_default_image)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            Log.e("NoteDetails", "로컬 파일 로드 실패: ${e?.message}", e)
                            // 다음 방법으로 시도
                            loadImageFromUrl()
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            Log.d("NoteDetails", "로컬 파일 로드 성공")
                            imageLoaded = true

                            // 실제 이미지 모드로 레이아웃 설정
                            val layoutParams = binding.imageUpload.layoutParams as ConstraintLayout.LayoutParams
                            layoutParams.width = dpToPx(300)
                            layoutParams.height = dpToPx(300)
                            binding.imageUpload.scaleType = ImageView.ScaleType.CENTER_CROP
                            layoutParams.topMargin = dpToPx(0)
                            binding.imageUpload.layoutParams = layoutParams
                            binding.imageUpload.adjustViewBounds = true
                            binding.imageUpload.requestLayout()

                            // ondevice stable diffusion에서 생성된 이미지인 경우 임시 파일 정리
                            if (intent.getBooleanExtra("GENERATED_IMAGE_SUCCESS", false)) {
                                binding.imageUpload.postDelayed({
                                    try {
                                        imageFile.delete()
                                        Log.d("NoteDetails", "임시 파일 삭제 완료")
                                    } catch (e: Exception) {
                                        Log.w("NoteDetails", "임시 파일 삭제 실패", e)
                                    }
                                }, 2000)
                            }

                            return false
                        }
                    })
                    .into(binding.imageUpload)

                if (imageLoaded) return
            } else {
                Log.e("NoteDetails", "이미지 파일이 존재하지 않음: $imageFilePath")
                // 다음 방법으로 진행
            }
        }

        // 3. 이미지 URL이 있는 경우
        loadImageFromUrl()
    }

    private fun loadImageFromUrl() {
        val imageUrl = intent.getStringExtra("IMAGE_URL")
        var imageLoaded = false

        if (!imageUrl.isNullOrEmpty()) {
            Log.d("NoteDetails", "이미지 URL 사용 시도: $imageUrl")

            Glide.with(this)
                .load(imageUrl)
                .error(R.drawable.ic_default_image)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("NoteDetails", "URL 이미지 로드 실패: ${e?.message}", e)
                        // 다음 방법으로 시도
                        loadImageFromByteArray()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d("NoteDetails", "URL 이미지 로드 성공")
                        imageLoaded = true
                        applyImageLayoutSettings()
                        return false
                    }
                })
                .into(binding.imageUpload)

            if (imageLoaded) return
        }

        // 4. byteArray가 있는 경우
        loadImageFromByteArray()
    }

    private fun loadImageFromByteArray() {
        val byteArray = intent.getByteArrayExtra("generatedImage")

        if (byteArray != null) {
            try {
                Log.d("NoteDetails", "비트맵 바이트 배열 사용 시도")
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                binding.imageUpload.setImageBitmap(bitmap)
                applyImageLayoutSettings()
                Log.d("NoteDetails", "바이트 배열 이미지 로드 성공")
            } catch (e: Exception) {
                Log.e("NoteDetails", "비트맵 디코딩 실패", e)
                // 이미지 업로드 버튼으로 설정
                binding.imageUpload.setImageResource(R.drawable.image_upload)
                applyImageLayoutSettings()
            }
        } else {
            // 모든 방법이 실패한 경우 이미지 업로드 버튼으로 설정
            Log.d("NoteDetails", "이미지를 찾을 수 없어 이미지 업로드 버튼 사용")
            binding.imageUpload.setImageResource(R.drawable.image_upload)
            applyImageLayoutSettings()
        }
    }

    private fun applyImageLayoutSettings() {
        // SHOW_IMAGE_UPLOAD_BUTTON 플래그 확인
        val showImageUploadButton = intent.getBooleanExtra("SHOW_IMAGE_UPLOAD_BUTTON", false)

        // 이미지 레이아웃 설정 적용
        val layoutParams = binding.imageUpload.layoutParams as ConstraintLayout.LayoutParams

        if (showImageUploadButton) {
            // 이미지 업로드 버튼 모드
            layoutParams.width = dpToPx(180)  // XML에서 정의된 크기
            layoutParams.height = dpToPx(180)
            binding.imageUpload.scaleType = ImageView.ScaleType.CENTER_INSIDE // 중앙 정렬
        } else {
            // 실제 이미지 표시 모드
            layoutParams.width = dpToPx(300)
            layoutParams.height = dpToPx(300)
            binding.imageUpload.scaleType = ImageView.ScaleType.CENTER_CROP // 크롭
        }

        layoutParams.topMargin = dpToPx(0)
        binding.imageUpload.layoutParams = layoutParams
        binding.imageUpload.adjustViewBounds = true
        binding.imageUpload.requestLayout()
    }

    private fun showImageChoicePopupAt(x: Int, y: Int) {
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.popup_image_choice, null)

        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.setBackgroundDrawable(ColorDrawable())
        popupWindow.isOutsideTouchable = true

        // 각 버튼 클릭 처리
        popupView.findViewById<LinearLayout>(R.id.btn_upload).setOnClickListener {
            openGallery()
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.btn_generate).setOnClickListener {
            // 노트 내용 가져오기
            val noteContent = intent.getStringExtra("MARKDOWN_CONTENT")
            val markdownContent = intent.getStringExtra("MARKDOWN_CONTENT")
            val conversationText = intent.getStringExtra("CONVERSATION_TEXT")
            val noteTitle = intent.getStringExtra("NOTE_TITLE")

            // 로그 추가
            Log.d("NoteDetails", "이미지 생성으로 전달할 데이터 - 마크다운: $markdownContent")
            Log.d("NoteDetails", "이미지 생성으로 전달할 데이터 - 대화: $conversationText")
            Log.d("NoteDetails", "이미지 생성으로 전달할 데이터 - 제목: $noteTitle")

            val intent = Intent(this, LoadingActivity::class.java)
            intent.putStringArrayListExtra("selectedTags", ArrayList(currentTagList))
            intent.putExtra("selectedPath", selectedPath)
            intent.putExtra("NOTE_CONTENT", markdownContent)
            intent.putExtra("MARKDOWN_CONTENT", markdownContent)
            intent.putExtra("CONVERSATION_TEXT", conversationText)
            intent.putExtra("NOTE_TITLE", noteTitle)

            popupWindow.dismiss()
            startActivity(intent)
            finish()
        }

        popupView.findViewById<LinearLayout>(R.id.btn_generate_offline).setOnClickListener {
            try {
                val intent = Intent()
                intent.setClassName(
                    "io.github.xororz.localdream",
                    "io.github.xororz.localdream.MainActivity"
                )

                // 노트 내용에서 프롬프트 추출/생성
                val noteContent = getIntent().getStringExtra("MARKDOWN_CONTENT") ?: ""
                val noteTitle = getIntent().getStringExtra("NOTE_TITLE") ?: ""
                val conversationText = getIntent().getStringExtra("CONVERSATION_TEXT") ?: ""

                // 프롬프트 생성 (노트 내용을 기반으로)
                val prompt = generatePromptFromContent(noteContent, noteTitle, conversationText)

                // 데이터 전달
                intent.putExtra("AUTO_GENERATE", true)
                intent.putExtra("PROMPT", prompt)

                // 원본 데이터들을 모두 전달하여 LocalDream에서 이미지 생성 후 다시 돌아올 때 유지
                intent.putExtra("NOTE_CONTENT", noteContent)
                intent.putExtra("MARKDOWN_CONTENT", noteContent)  // NOTE_CONTENT와 동일한 값
                intent.putExtra("CONVERSATION_TEXT", conversationText)
                intent.putExtra("NOTE_TITLE", noteTitle)
                intent.putExtra("SOURCE_APP", "tokkit")

                // 현재 NoteDetailsActivity의 추가 상태 정보도 전달
                intent.putStringArrayListExtra("SELECTED_TAGS", ArrayList(currentTagList))
                intent.putExtra("SELECTED_PATH", selectedPath)
                intent.putExtra("SELECTED_DIRECTORY_ID", selectedDirectoryId)
                intent.putExtra("IS_PUBLIC", isPublic)

                // 기존 이미지 정보도 전달 (필요한 경우)
                val existingImageUrl = getIntent().getStringExtra("IMAGE_URL")
                val existingS3ImageKey = getIntent().getStringExtra("S3_IMAGE_KEY")
                if (!existingImageUrl.isNullOrEmpty()) {
                    intent.putExtra("EXISTING_IMAGE_URL", existingImageUrl)
                }
                if (!existingS3ImageKey.isNullOrEmpty()) {
                    intent.putExtra("EXISTING_S3_IMAGE_KEY", existingS3ImageKey)
                }

                Log.d("NoteDetails", "LocalDream으로 전달하는 데이터:")
                Log.d("NoteDetails", "- 프롬프트: $prompt")
                Log.d("NoteDetails", "- 노트 내용: ${noteContent.take(50)}...")
                Log.d("NoteDetails", "- 마크다운 내용: ${noteContent.take(50)}...")
                Log.d("NoteDetails", "- 대화 내용: ${conversationText.take(50)}...")
                Log.d("NoteDetails", "- 노트 제목: $noteTitle")
                Log.d("NoteDetails", "- 태그: $currentTagList")
                Log.d("NoteDetails", "- 저장 경로: $selectedPath")

                startActivity(intent)
                popupWindow.dismiss()

                // 결과를 기다리기 위해 finish() 안함
                // finish()
            } catch (e: Exception) {
                CustomToastUtil.showToast(
                    context = this@NoteDetailsActivity,
                    message = "오프라인 AI 앱을 찾을 수 없습니다",
                    iconResId = R.drawable.ic_bot
                )
                Log.e("NoteDetails", "LocalDream 앱 실행 실패", e)
            }
        }

      
        // 터치한 좌표를 기준으로 팝업 띄우기 (왼쪽 상단 정렬)
        popupWindow.showAtLocation(binding.root, 0, x, y)
    }

    // 노트 내용에서 프롬프트를 생성하는 함수
    private fun generatePromptFromContent(content: String, title: String, conversation: String): String {
        // 간단한 프롬프트 생성 로직
        return when {
            title.isNotEmpty() -> title
            content.length > 100 -> content.substring(0, 100) + "..."
            content.isNotEmpty() -> content
            conversation.isNotEmpty() -> conversation.substring(0, minOf(100, conversation.length))
            else -> "beautiful artwork"
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_GALLERY_IMAGE)
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // 새로운 인텐트를 현재 액티비티의 인텐트로 설정

        Log.d("NoteDetails", "onNewIntent 호출됨")

        // 외부 앱에서 생성된 이미지 결과 처리
        if (intent.getBooleanExtra("GENERATED_IMAGE_SUCCESS", false)) {
            val imagePath = intent.getStringExtra("GENERATED_IMAGE_PATH")

            Log.d("NoteDetails", "이미지 생성 결과 수신 - 경로: $imagePath")

            // 전달받은 추가 데이터 처리
            val noteContent = intent.getStringExtra("NOTE_CONTENT")
            val markdownContent = intent.getStringExtra("MARKDOWN_CONTENT")
            val noteTitle = intent.getStringExtra("NOTE_TITLE")
            val conversationText = intent.getStringExtra("CONVERSATION_TEXT")

            // NoteDetailsActivity 상태 정보들도 복원
            val selectedTags = intent.getStringArrayListExtra("SELECTED_TAGS")
            val selectedPath = intent.getStringExtra("SELECTED_PATH")
            val selectedDirectoryId = intent.getIntExtra("SELECTED_DIRECTORY_ID", -1)
            val isPublic = intent.getBooleanExtra("IS_PUBLIC", true)

            Log.d("NoteDetails", "추가 데이터 수신:")
            Log.d("NoteDetails", "- 노트 내용: ${noteContent?.take(50)}...")
            Log.d("NoteDetails", "- 마크다운: ${markdownContent?.take(50)}...")
            Log.d("NoteDetails", "- 제목: $noteTitle")
            Log.d("NoteDetails", "- 대화: ${conversationText?.take(50)}...")
            Log.d("NoteDetails", "- 선택된 태그: $selectedTags")
            Log.d("NoteDetails", "- 선택된 경로: $selectedPath")
            Log.d("NoteDetails", "- 디렉토리 ID: $selectedDirectoryId")
            Log.d("NoteDetails", "- 공개 설정: $isPublic")

            // 상태 복원
            selectedTags?.let {
                currentTagList = it.toMutableList()
                if (it.isNotEmpty()) {
                    binding.tagContent.visibility = View.INVISIBLE
                    binding.tagContainerInNote.visibility = View.VISIBLE
                    renderSelectedTags(it)
                }
            }

            selectedPath?.let {
                this.selectedPath = it
                binding.storageDetail.text = it
            }

            if (selectedDirectoryId != -1) {
                this.selectedDirectoryId = selectedDirectoryId
            }

            // 공개 설정 복원
            this.isPublic = isPublic
            if (isPublic) {
                binding.visibilityRadioGroup.check(R.id.publicOption)
            } else {
                binding.visibilityRadioGroup.check(R.id.privateOption)
            }

            // 이미지 처리
            if (!imagePath.isNullOrEmpty()) {
                val imageFile = File(imagePath)
                if (imageFile.exists()) {
                    Log.d("NoteDetails", "외부 앱에서 생성된 이미지 수신: $imagePath")

                    // 이미지 파일을 비트맵으로 로드하여 저장
                    generatedImageBitmap = BitmapFactory.decodeFile(imagePath)
                    generatedImagePath = imagePath

                    // 이미지를 ImageView에 표시
                    Glide.with(this)
                        .load(imageFile)
                        .placeholder(binding.imageUpload.drawable) // 현재 이미지를 플레이스홀더로 사용
                        .error(R.drawable.ic_default_image)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                Log.e("NoteDetails", "생성된 이미지 로드 실패: ${e?.message}", e)
                                CustomToastUtil.showToast(
                                    context = this@NoteDetailsActivity,
                                    message = "이미지 로드에 실패했습니다.",
                                    iconResId = R.drawable.ic_bot
                                )
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                Log.d("NoteDetails", "생성된 이미지 로드 성공")

                                // 레이아웃 설정 적용 (실제 이미지 모드로 변경)
                                val layoutParams = binding.imageUpload.layoutParams as ConstraintLayout.LayoutParams
                                layoutParams.width = dpToPx(300)
                                layoutParams.height = dpToPx(300)
                                binding.imageUpload.scaleType = ImageView.ScaleType.CENTER_CROP
                                layoutParams.topMargin = dpToPx(0)
                                binding.imageUpload.layoutParams = layoutParams
                                binding.imageUpload.adjustViewBounds = true
                                binding.imageUpload.requestLayout()

                                // 성공 메시지 표시
                                //Toast.makeText(this@NoteDetailsActivity, "이미지가 생성되었습니다!", Toast.LENGTH_SHORT).show()

                                // 임시 파일 정리를 지연시킴 (이미지 로드 후)
                                val isTempFile = intent.getBooleanExtra("IS_TEMP_FILE", false)
                                if (isTempFile) {
                                    binding.imageUpload.postDelayed({
                                        try {
                                            if (imageFile.exists()) {
                                                val deleted = imageFile.delete()
                                                Log.d("NoteDetails", "임시 파일 삭제 ${if (deleted) "성공" else "실패"}: ${imageFile.absolutePath}")
                                            }
                                        } catch (e: Exception) {
                                            Log.w("NoteDetails", "임시 파일 삭제 실패", e)
                                        }
                                    }, 3000)
                                }

                                return false
                            }
                        })
                        .into(binding.imageUpload)

                } else {
                    Log.e("NoteDetails", "생성된 이미지 파일을 찾을 수 없음: $imagePath")
                    CustomToastUtil.showToast(
                        context = this,
                        message = "생성된 이미지를 불러올 수 없습니다.",
                        iconResId = R.drawable.ic_bot
                    )
                }
            } else {
                Log.e("NoteDetails", "이미지 경로가 비어있음")
                CustomToastUtil.showToast(
                    context = this,
                    message = "이미지 경로를 받지 못했습니다.",
                    iconResId = R.drawable.ic_bot
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 태그 관리 결과 처리
        if (requestCode == 101 && resultCode == RESULT_OK) {
            val tagList = data?.getStringArrayListExtra("selectedTags") ?: return

            if (tagList.isNotEmpty()) {
                binding.tagContent.visibility = View.INVISIBLE
                binding.tagContainerInNote.visibility = View.VISIBLE
                renderSelectedTags(tagList)
                Log.d("NoteDetails", "태그 목록 업데이트: $tagList")
            }
        }

        // 저장 위치 결과 처리
        if (requestCode == 102 && resultCode == RESULT_OK) {
            val path = data?.getStringExtra("selectedPath")
            selectedDirectoryId = data?.getIntExtra("selectedDirectoryId", -1)
            if (selectedDirectoryId == -1) selectedDirectoryId = null

            if (!path.isNullOrEmpty()) {
                binding.storageDetail.text = path
                selectedPath = path

                // 폴더 구조 정보 받기
                val folderStructure = data.getStringArrayListExtra("folderStructure")
                Log.d("NoteDetails", "저장 위치 업데이트: $path")
                Log.d("NoteDetails", "폴더 구조: $folderStructure")
                Log.d("NoteDetails", "디렉토리 ID: $selectedDirectoryId")

            }
        }

        // 갤러리 이미지 선택 결과 처리
        if (requestCode == REQUEST_GALLERY_IMAGE && resultCode == RESULT_OK) {
            val selectedImageUri = data?.data
            if (selectedImageUri != null) {
                // 선택한 이미지 표시
                Glide.with(this)
                    .load(selectedImageUri)
                    .into(binding.imageUpload)

                applyImageLayoutSettings()
            }
        }
    }

    private fun renderSelectedTags(tags: List<String>) {
        currentTagList = tags.toMutableList()
        val tagContainer = binding.tagContainerInNote
        tagContainer.removeAllViews()

        val inflater = LayoutInflater.from(this)
        for (tag in tags) {
            val chip = inflater.inflate(R.layout.item_chip, tagContainer, false) as TextView
            chip.text = "# $tag"
            tagContainer.addView(chip)
        }
    }

    // S3에 이미지 업로드하는 함수
    private suspend fun uploadImageToS3(bitmap: Bitmap): String? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("NoteDetails", "S3 이미지 업로드 시작")

                // 1. 프리사인드 URL 발급 받기
                val s3ApiService = RetrofitClient.createService(S3ApiService::class.java)
                val fileName = "generated_image_${System.currentTimeMillis()}.jpg"

                val response = s3ApiService.getPreSignedUrl("banner", fileName)

                if (!response.isSuccess) {
                    Log.e("NoteDetails", "프리사인드 URL 발급 실패: ${response.message}")
                    return@withContext null
                }

                val preSignedUrl = response.result.preSignedUrl
                val imageKey = response.result.imageKey

                Log.d("NoteDetails", "프리사인드 URL 발급 성공: $imageKey")

                // 2. 비트맵을 JPEG 바이트 배열로 변환
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                val imageBytes = outputStream.toByteArray()

                // 3. S3에 업로드
                val client = OkHttpClient.Builder().build()
                val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaType())

                val request = Request.Builder()
                    .url(preSignedUrl)
                    .put(requestBody)
                    .build()

                val uploadResponse = client.newCall(request).execute()

                if (uploadResponse.isSuccessful) {
                    Log.d("NoteDetails", "S3 업로드 성공: $imageKey")
                    withContext(Dispatchers.Main) {
                        //Toast.makeText(this@NoteDetailsActivity, "이미지가 서버에 업로드되었습니다", Toast.LENGTH_SHORT).show()
                    }
                    return@withContext imageKey
                } else {
                    Log.e("NoteDetails", "S3 업로드 실패: ${uploadResponse.code} - ${uploadResponse.message}")
                    return@withContext null
                }

            } catch (e: Exception) {
                Log.e("NoteDetails", "S3 업로드 중 예외 발생", e)
                return@withContext null
            }
        }
    }


    private fun saveNoteToServer(
        noteTitle: String,
        markdownContent: String,
        conversationText: String,
        isPublic: Boolean,
        directoryId: Int? = null,
        s3ImageKey: String? = null,
    ) {
        // 빈 데이터 검사
        if (markdownContent.isBlank()) {
            Log.e("NoteDetails", "마크다운 내용이 비어있어 저장할 수 없습니다")

            //Toast.makeText(this, "저장할 노트 내용이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (noteTitle.isBlank()) {
            Log.e("NoteDetails", "노트 제목이 비어있어 저장할 수 없습니다")
            CustomToastUtil.showToast(
                context = this,
                message = "노트 제목이 필요합니다.",
                iconResId = R.drawable.ic_bot
            )
            return
        }

        // 로그 추가
        Log.d("NoteDetails", "저장할 데이터 - 제목: $noteTitle")
        Log.d("NoteDetails", "저장할 데이터 - 마크다운 내용 길이: ${markdownContent.length}")
        Log.d("NoteDetails", "저장할 데이터 - 대화 내용 길이: ${conversationText.length}")
        Log.d("NoteDetails", "저장할 데이터 - S3 이미지 키: $s3ImageKey")
        Log.d("NoteDetails", "저장할 데이터 - LocalDream 생성 이미지: ${generatedImageBitmap != null}")
        Log.d("NoteDetails", "저장할 데이터 - 태그 목록: $currentTagList")
        Log.d("NoteDetails", "저장할 데이터 - 디렉토리 ID: $directoryId")

        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            try {
                var finalImageKey = s3ImageKey

                // LocalDream에서 생성된 이미지가 있는 경우 S3에 업로드
                if (generatedImageBitmap != null && finalImageKey.isNullOrEmpty()) {
                    Log.d("NoteDetails", "LocalDream 생성 이미지를 S3에 업로드 시도")
                    //Toast.makeText(this@NoteDetailsActivity, "이미지를 업로드 중입니다...", Toast.LENGTH_SHORT).show()

                    finalImageKey = uploadImageToS3(generatedImageBitmap!!)

                    if (finalImageKey == null) {
                        Log.e("NoteDetails", "이미지 업로드 실패, 기본 이미지 키 사용")
                        //Toast.makeText(this@NoteDetailsActivity, "이미지 업로드에 실패했습니다. 기본 이미지로 저장됩니다.", Toast.LENGTH_SHORT).show()
                        finalImageKey = "profile-images/test-image_c37fb6f2-2fec-4d41-8f06-53d226de2ac6"
                    }
                }

                // 이미지 키 설정 - 우선순위: 업로드된 이미지 키 > 기존 S3 키 > 기본 키
                val imageKey = finalImageKey
                    ?: intent.getStringExtra("IMAGE_URL")
                    ?: "profile-images/test-image_c37fb6f2-2fec-4d41-8f06-53d226de2ac6"

                // 노트 ID 생성 (UUID)
                val noteId = UUID.randomUUID().toString()

                // 다음 복습 시간 설정 (현재 시간 + 1일)
                val formatter = DateTimeFormatter.ISO_DATE_TIME
                val nextReviewTime = LocalDateTime.now().plusDays(1).format(formatter)

                // 요청 객체 생성
                val noteRequest = NoteCreateRequest(
                    id = noteId,
                    title = noteTitle,
                    content = markdownContent,
                    isPublic = isPublic,
                    directoryId = directoryId,
                    bannerImageKey = imageKey,
                    conversationLog = conversationText,
                    stage = "STAGE0",
                    tags = currentTagList,
                    nextReviewAt = nextReviewTime
                )

                // 리스트로 만들어서 보내야 함
                val noteRequestList = listOf(noteRequest)

                // 요청 바디를 JSON 문자열로 변환하여 로그 출력 (디버깅용)
                val gson = Gson()
                val requestJson = gson.toJson(noteRequestList)
                Log.d("NoteDetails", "API 요청 JSON: $requestJson")

                val memberId = 1L
                val batch = false // 단일 노트 생성이므로 false

                // API 호출
                Log.d("NoteDetails", "API 호출 직전 - memberId: $memberId, batch: $batch")
                val api = RetrofitClient.noteApi

                val response = withContext(Dispatchers.IO) {
                    Log.d("NoteDetails", "API 호출 실행")
                    // batch 파라미터와 함께 API 호출
                    api.createNote(memberId = memberId, batch = batch, notes = noteRequestList)
                }
                Log.d("NoteDetails", "API 호출 완료: ${response.code}, ${response.message}")

                if (response.isSuccess) {
                    // 저장 성공
                    Log.d("NoteDetails", "노트 저장 성공: ${response.result}")

                    // 대화 내용 초기화
                    ConversationManager.clearMessages()
                    ConversationManager.clearSavedConversation(this@NoteDetailsActivity)
                    ConversationManager.startNewSession()
                    Log.d("NoteDetails", "대화 내용 초기화 완료")

                    // 성공 메시지 표시
                    CustomToastUtil.showToast(
                        context = this@NoteDetailsActivity,
                        message = "노트가 저장되었습니다",
                        iconResId = R.drawable.ic_bot
                    )

                    // 메인 화면으로 돌아가기
                    val intent = Intent(this@NoteDetailsActivity, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    finish()
                } else {
                    // 저장 실패
                    Log.e("NoteDetails", "노트 저장 실패: ${response.message}")
                    CustomToastUtil.showToast(
                        context = this@NoteDetailsActivity,
                        message = "노트 저장에 실패했습니다: ${response.message}",
                        iconResId = R.drawable.ic_bot
                    )
                }
            } catch (e: HttpException) {
                // HTTP 예외 처리
                val errorCode = e.code()
                val errorBody = e.response()?.errorBody()?.string() ?: "오류 내용 없음"

                Log.e("NoteDetails", "HTTP 오류 발생: 코드=$errorCode, 응답 본문=$errorBody", e)
                CustomToastUtil.showToast(
                    context = this@NoteDetailsActivity,
                    message = "서버 오류가 발생했습니다 (코드: $errorCode)",
                    iconResId = R.drawable.ic_bot
                )
            } catch (e: Exception) {
                // 일반 예외 처리
                Log.e("NoteDetails", "노트 저장 중 오류 발생", e)
                CustomToastUtil.showToast(
                    context = this@NoteDetailsActivity,
                    message = "오류 발생: ${e.message}",
                    iconResId = R.drawable.ic_bot
                )
            }
        }
    }
}