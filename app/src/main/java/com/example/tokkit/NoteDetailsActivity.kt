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
import java.util.UUID


class NoteDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNoteDetailsBinding

    private var currentTagList = mutableListOf<String>()
    private var selectedPath: String? = null
    private val REQUEST_GALLERY_IMAGE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent에서 데이터 가져오기
        val markdownContent = intent.getStringExtra("MARKDOWN_CONTENT")
        val conversationText = intent.getStringExtra("CONVERSATION_TEXT")
        val noteTitle = intent.getStringExtra("NOTE_TITLE") ?: "대화 요약"

        // 로그로 데이터 확인
        Log.d("NoteDetails", "마크다운 내용: $markdownContent")
        Log.d("NoteDetails", "대화 내용: $conversationText")
        Log.d("NoteDetails", "노트 제목: $noteTitle")

        // 공개 설정 기본값 초기화 (기본값-> 전체 공개)
        var isPublic = true

        // 공개 설정 라디오 버튼 리스너 설정
        binding.visibilityRadioGroup.setOnCheckedChangeListener { group, checkedId ->
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

        // 저장 버튼 (새로 추가)
        binding.saveNoteButton.setOnClickListener {
            // 현재 공개 설정, 태그, 저장 위치 정보 로그 출력
            Log.d("NoteDetails", "저장 시점 정보:")
            Log.d("NoteDetails", "- 공개 설정: is_public = $isPublic")
            Log.d("NoteDetails", "- 태그 목록: $currentTagList")
            Log.d("NoteDetails", "- 저장 위치: $selectedPath")

            // API 호출 시작 로그 추가
            Log.d("NoteDetails", "노트 저장 API 호출 시작")

            // 저장 시도 메시지 표시
            Toast.makeText(this, "노트를 저장 중입니다...", Toast.LENGTH_SHORT).show()

            saveNoteToServer(
                noteTitle = intent.getStringExtra("NOTE_TITLE") ?: "대화 요약",
                markdownContent = intent.getStringExtra("MARKDOWN_CONTENT") ?: "",
                conversationText = intent.getStringExtra("CONVERSATION_TEXT") ?: "",
                isPublic = isPublic,
                directoryName = selectedPath ?: "기본 경로"
            )

            // 대화 내용 초기화
            ConversationManager.clearMessages()
            ConversationManager.clearSavedConversation(this)
            ConversationManager.startNewSession()
            Log.d("NoteDetails", "대화 내용 초기화 완료")

            Toast.makeText(this, "노트가 저장되었습니다", Toast.LENGTH_SHORT).show()

            // 메인 화면으로 돌아가기
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)  // 스택의 최상위로 MainActivity 가져오기
            startActivity(intent)
            finish()  // 현재 활동 종료
        }


        val byteArray = intent.getByteArrayExtra("generatedImage")
        if (byteArray != null) {
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            binding.imageUpload.setImageBitmap(bitmap)

            // 크기 조정
            val layoutParams = binding.imageUpload.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.width = dpToPx(300)
            layoutParams.height = dpToPx(300)

            // marginTop 24dp로 변경
            layoutParams.topMargin = dpToPx(0)

            binding.imageUpload.layoutParams = layoutParams

            // 스케일 설정
            binding.imageUpload.scaleType = ImageView.ScaleType.CENTER_CROP
            binding.imageUpload.adjustViewBounds = true
            binding.imageUpload.requestLayout()
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

        binding.tagForward.setOnClickListener {
            val intent = Intent(this, TagManageActivity::class.java)
            intent.putStringArrayListExtra("existingTags", ArrayList(currentTagList))
            startActivityForResult(intent, 101)
        }

        //저장 위치 화살표 버튼
        binding.storageForward.setOnClickListener{
            val intent = Intent(this, SaveLocationActivity::class.java)
            startActivityForResult(intent, 102)
        }
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
            // TODO: 이미지 생성 로직 구현 ( Stable Diffusion )
            val intent = Intent(this, LoadingActivity::class.java)
            intent.putStringArrayListExtra("selectedTags", ArrayList(currentTagList))
            intent.putExtra("selectedPath", selectedPath)
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
        if (requestCode == 101 && resultCode == RESULT_OK) {
            val tagList = data?.getStringArrayListExtra("selectedTags") ?: return
            if (tagList.isNotEmpty()) {
                binding.tagContent.visibility = View.INVISIBLE      // 안내 문구 숨김 (공간 유지)
                binding.tagContainerInNote.visibility = View.VISIBLE  // 칩 영역 표시
                renderSelectedTags(tagList)
                Log.d("NoteDetails", "태그 목록 업데이트: $tagList")
            }
        }
        if (requestCode == 102 && resultCode == RESULT_OK) {
            val path = data?.getStringExtra("selectedPath")
            if (!path.isNullOrEmpty()) {
                binding.storageDetail.text = path
                selectedPath = path

                // 폴더 구조 정보 받기
                val folderStructure = data.getStringArrayListExtra("folderStructure")
                Log.d("NoteDetails", "저장 위치 업데이트: $path")
                Log.d("NoteDetails", "폴더 구조: $folderStructure")
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
        directoryName: String
    ) {
        // 고정된 이미지 URL (요구사항에 따름)
        val imageUrl = "profile-images/test-image_c37fb6f2-2fec-4d41-8f06-53d226de2ac6"

        // 노트 ID 생성 (UUID)
        val noteId = UUID.randomUUID().toString()

        // 요청 객체 생성
        val noteRequest = NoteCreateRequest(
            id = noteId,
            title = noteTitle,
            content = markdownContent,
            isPublic = isPublic,
            directoryName = directoryName,
            imageUrl = imageUrl,
            conversationLog = conversationText,
            stage = "STAGE0"
        )

        // 요청 바디를 JSON 문자열로 변환하여 로그 출력 (디버깅용)
        val gson = Gson()
        val requestJson = gson.toJson(listOf(noteRequest))
        Log.d("NoteDetails", "API 요청 JSON: $requestJson")

        // API 호출
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            try {
                val api = RetrofitClient.noteApi

                Log.d("NoteDetails", "API 호출 직전")
                val response = withContext(Dispatchers.IO) {
                    Log.d("NoteDetails", "API 호출 실행")
                    api.createNote(listOf(noteRequest))
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