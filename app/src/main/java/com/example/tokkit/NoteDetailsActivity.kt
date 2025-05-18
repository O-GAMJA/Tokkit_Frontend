package com.example.tokkit

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.example.tokkit.databinding.ActivityNoteDetailsBinding
import com.example.tokkit.genie.ConversationManager
import com.example.tokkit.data.remote.api.NoteApiService
import com.example.tokkit.data.remote.model.ApiResponse
import com.example.tokkit.data.remote.model.NoteCreateRequest
import com.example.tokkit.util.RetrofitClient
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.File
import java.util.UUID


class NoteDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNoteDetailsBinding

    private var currentTagList = mutableListOf<String>()
    private var selectedPath: String? = null
    private val REQUEST_GALLERY_IMAGE = 1001
    private var isPublic = true
    private var selectedDirectoryId: Int? = null


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
            Toast.makeText(this, "노트를 저장 중입니다...", Toast.LENGTH_SHORT).show()

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

        // 이미지 표시 처리
        setupImageDisplay(s3ImageKey, imageUrl)
    }

    private fun setupImageDisplay(s3ImageKey: String?, imageUrl: String?) {
        // 1. S3 이미지 키가 있는 경우
        if (!s3ImageKey.isNullOrEmpty()) {
            Log.d("NoteDetails", "S3 이미지 키를 사용하여 이미지 표시: $s3ImageKey")

            // S3 이미지 URL 구성 - 실제 사용되는 URL 형식으로 변경 필요
            val fullImageUrl = "http://52.79.86.14:8080/image/$s3ImageKey"

            Glide.with(this)
                .load(fullImageUrl)
                .into(binding.imageUpload)

            applyImageLayoutSettings()
            return
        }

        // 2. 이미지 파일 경로가 있는 경우
        val imageFilePath = intent.getStringExtra("IMAGE_FILE_PATH")
        if (!imageFilePath.isNullOrEmpty()) {
            val imageFile = File(imageFilePath)
            if (imageFile.exists()) {
                Log.d("NoteDetails", "로컬 이미지 파일 표시: $imageFilePath")

                Glide.with(this)
                    .load(imageFile)
                    .into(binding.imageUpload)

                applyImageLayoutSettings()
                return
            } else {
                Log.e("NoteDetails", "이미지 파일이 존재하지 않음: $imageFilePath")
            }
        }

        // 3. 이미지 URL이 있는 경우
        if (!imageUrl.isNullOrEmpty()) {
            Log.d("NoteDetails", "이미지 URL 사용: $imageUrl")

            Glide.with(this)
                .load(imageUrl)
                .into(binding.imageUpload)

            applyImageLayoutSettings()
            return
        }

        // 4. byteArray가 있는 경우 (이전 방식)
        val byteArray = intent.getByteArrayExtra("generatedImage")
        if (byteArray != null) {
            try {
                Log.d("NoteDetails", "비트맵 바이트 배열 사용")
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                binding.imageUpload.setImageBitmap(bitmap)
                applyImageLayoutSettings()
            } catch (e: Exception) {
                Log.e("NoteDetails", "비트맵 디코딩 실패", e)
            }
        }
    }

    private fun applyImageLayoutSettings() {
        // 이미지 레이아웃 설정 적용
        val layoutParams = binding.imageUpload.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.width = dpToPx(300)
        layoutParams.height = dpToPx(300)
        layoutParams.topMargin = dpToPx(0)
        binding.imageUpload.layoutParams = layoutParams

        // 스케일 설정
        binding.imageUpload.scaleType = ImageView.ScaleType.CENTER_CROP
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

        // 터치한 좌표를 기준으로 팝업 띄우기 (왼쪽 상단 정렬)
        popupWindow.showAtLocation(binding.root, 0, x, y)
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
            Toast.makeText(this, "저장할 노트 내용이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (noteTitle.isBlank()) {
            Log.e("NoteDetails", "노트 제목이 비어있어 저장할 수 없습니다")
            Toast.makeText(this, "노트 제목이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 로그 추가
        Log.d("NoteDetails", "저장할 데이터 - 제목: $noteTitle")
        Log.d("NoteDetails", "저장할 데이터 - 마크다운 내용 길이: ${markdownContent.length}")
        Log.d("NoteDetails", "저장할 데이터 - 대화 내용 길이: ${conversationText.length}")
        Log.d("NoteDetails", "저장할 데이터 - S3 이미지 키: $s3ImageKey")
        Log.d("NoteDetails", "저장할 데이터 - 태그 목록: $currentTagList")
        Log.d("NoteDetails", "저장할 데이터 - 디렉토리 ID: $directoryId")

        // 이미지 키 설정
        val imageKey = if (!s3ImageKey.isNullOrEmpty()) {
            s3ImageKey
        } else {
            intent.getStringExtra("IMAGE_URL") ?: "profile-images/test-image_c37fb6f2-2fec-4d41-8f06-53d226de2ac6"
        }

        // 노트 ID 생성 (UUID)
        val noteId = UUID.randomUUID().toString()

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
            tags = currentTagList
        )

        // 리스트로 만들어서 보내야 함
        val noteRequestList = listOf(noteRequest)

        // 요청 바디를 JSON 문자열로 변환하여 로그 출력 (디버깅용)
        val gson = Gson()
        val requestJson = gson.toJson(noteRequestList)
        Log.d("NoteDetails", "API 요청 JSON: $requestJson")

        val memberId = 1L

        // API 호출
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            try {
                val api = RetrofitClient.noteApi

                Log.d("NoteDetails", "API 호출 직전")
                val response = withContext(Dispatchers.IO) {
                    Log.d("NoteDetails", "API 호출 실행")
                    // 리스트로 전달
                    api.createNote(memberId, noteRequestList)
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
                    Toast.makeText(this@NoteDetailsActivity, "노트가 저장되었습니다", Toast.LENGTH_SHORT).show()

                    // 메인 화면으로 돌아가기
                    val intent = Intent(this@NoteDetailsActivity, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    finish()
                } else {
                    // 저장 실패
                    Log.e("NoteDetails", "노트 저장 실패: ${response.message}")
                    Toast.makeText(this@NoteDetailsActivity, "노트 저장에 실패했습니다: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: HttpException) {
                // HTTP 예외 처리
                val errorCode = e.code()
                val errorBody = e.response()?.errorBody()?.string() ?: "오류 내용 없음"

                Log.e("NoteDetails", "HTTP 오류 발생: 코드=$errorCode, 응답 본문=$errorBody", e)
                Toast.makeText(this@NoteDetailsActivity, "서버 오류가 발생했습니다 (코드: $errorCode)", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // 일반 예외 처리
                Log.e("NoteDetails", "노트 저장 중 오류 발생", e)
                Toast.makeText(this@NoteDetailsActivity, "오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}