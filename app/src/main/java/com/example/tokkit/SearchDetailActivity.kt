package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.tokkit.adapter.CommentAdapter
import com.example.tokkit.adapter.SimilarNoteAdapter
import com.example.tokkit.data.remote.model.BookmarkStatus
import com.example.tokkit.data.remote.model.Note
import com.example.tokkit.databinding.ActivitySearchDetailBinding
import com.example.tokkit.model.Comment
import com.example.tokkit.util.RetrofitClient
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.noties.markwon.Markwon
import kotlinx.coroutines.launch

class SearchDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchDetailBinding
    private var bookmarkCount = 3 // 초기 북마크 카운트
    private var isBookmarked = false // 북마크 상태
    private lateinit var dotsIndicator: List<ImageView>
    private var currentNote: Note? = null // 현재 노트 정보
    private var isModified = false // 노트 수정 여부 플래그

    private val noteViewModel: NoteViewModel by viewModels()
    private var isEditMode = false
    private var currentNoteId: String? = null

    private val MENU_EDIT_ID = 1
    private val MENU_SAVE_ID = 2
    private val MENU_DELETE_ID = 3

    // 댓글 목록 데이터 (전역 변수로 변경)
    private val commentList = mutableListOf(
        Comment("홍길동", "1시간 전", "이 글이 매우 도움이 되었습니다. 특히 OSI 7계층 설명이 이해하기 쉬웠어요!", 5),
        Comment("김철수", "3시간 전", "TCP와 UDP의 차이점을 잘 설명해주셨네요. 감사합니다.", 3),
        Comment("이영희", "어제", "네트워크 공부하는데 좋은 참고자료가 될 것 같습니다. 잘 봤습니다!", 7)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val noteId = intent.getStringExtra("NOTE_ID") ?: return
        currentNoteId = noteId

        noteViewModel.loadNoteById(noteId)

        noteViewModel.loadSimilarNotes(noteId)

        noteViewModel.selectedNote.observe(this) { note ->
            currentNote = note
            if (note != null) {
                // 더보기 버튼
                binding.btnMore.setOnClickListener { view ->
                    showPopupMenu(view, note.id)
                }

                // 제목
                binding.tvTitle.text = note.title ?: "(제목 없음)"

                // 마크다운 내용
                val markwon = Markwon.create(this)
                val safeContent = note.content ?: ""
                markwon.setMarkdown(binding.tvContent, safeContent)

                // 이미지
                val imageUrl = note.imageUrl
                if (!imageUrl.isNullOrBlank()) {
                    Glide.with(this).load(imageUrl).into(binding.ivArticleImage)
                } else {
                    binding.ivArticleImage.setImageDrawable(null)
                }

                // 북마크 상태
                val bookmarkStatus: BookmarkStatus = note.bookmarkStatus ?: BookmarkStatus(0, false)
                bookmarkCount = bookmarkStatus.count
                isBookmarked = bookmarkStatus.clicked
                binding.bookmarkCount.text = bookmarkCount.toString()
                binding.btnBookmark.setImageResource(
                    if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark
                )

                // 이모지 상태
                val emojiStatus = note.emojiStatus ?: return@observe
                binding.likeCount.text = emojiStatus.count["like"]?.toString() ?: "0"
                binding.heartCount.text = emojiStatus.count["thumbsUp"]?.toString() ?: "0"
                binding.thinkingCount.text = emojiStatus.count["thinking"]?.toString() ?: "0"
                binding.fireCount.text = emojiStatus.count["fire"]?.toString() ?: "0"
                binding.hundredCount.text = emojiStatus.count["hundred"]?.toString() ?: "0"
            } else {
                Toast.makeText(this, "노트를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        noteViewModel.similarNotes.observe(this) { similarNotes ->
            if (similarNotes.isNotEmpty()) {
                val adapter = SimilarNoteAdapter(similarNotes) { noteItem ->
                    val intent = Intent(this, SearchDetailActivity::class.java)
                    intent.putExtra("NOTE_ID", noteItem.noteId)
                    startActivity(intent)
                }

                binding.relatedArticlesViewPager.adapter = adapter
                binding.relatedArticlesViewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL

                setupDotIndicators(similarNotes.size)
                binding.relatedArticlesViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        updateDots(position)
                    }
                })
            }
        }

        // 뒤로가기 버튼 설정
        binding.btnBack.setOnClickListener {
            val result = Intent().apply {
                putExtra("noteModified", true)
            }
            setResult(RESULT_OK, result)
            finish()
        }

        // 북마크 버튼 설정
        setupBookmarkButton()

        // 이모티콘 버튼 기능
        setupReactionButtons()

        // 댓글 버튼 클릭 이벤트 설정
        binding.commentButton.setOnClickListener {
            showCommentBottomSheet()
        }
    }

    private fun showPopupMenu(view: View, noteId: String) {
        val popupMenu = PopupMenu(this, view)
        val menu = popupMenu.menu

        // 동적 메뉴 텍스트 변경
        if (isEditMode) {
            menu.add(0, MENU_SAVE_ID, 0, "저장")
        } else {
            menu.add(0, MENU_EDIT_ID, 0, "수정")
        }
        menu.add(0, MENU_DELETE_ID, 1, "삭제")

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_EDIT_ID -> {
                    enterEditMode()
                    true
                }
                MENU_SAVE_ID -> {
                    saveEditedNote()
                    true
                }
                MENU_DELETE_ID -> {
                    deleteNote(noteId)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun enterEditMode() {
        isEditMode = true

        // 제목 수정 가능
        binding.tvTitle.visibility = View.GONE
        binding.etTitleEditor.visibility = View.VISIBLE
        binding.modeExplain.visibility = View.VISIBLE
        binding.etTitleEditor.setText(binding.tvTitle.text.toString())

        // 본문 수정 가능
        binding.tvContent.visibility = View.GONE
        binding.etContentEditor.visibility = View.VISIBLE
        binding.etContentEditor.setText(currentNote?.content ?: "")

        // 불필요한 뷰 숨기기
        binding.bookmarkContainer.visibility = View.GONE
        binding.reactionLayout.visibility = View.GONE
        binding.tvRelatedTitle.visibility = View.GONE
        binding.relatedArticlesViewPager.visibility = View.GONE
        binding.dotsIndicator.visibility = View.GONE
    }

    private fun saveEditedNote() {
        val noteId = currentNoteId ?: return
        val newContent = binding.etContentEditor.text.toString()
        val newTitle = binding.etTitleEditor.text.toString()

        val patchData = mapOf(
            "title" to newTitle,
            "content" to newContent
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.noteApi.updateNote(noteId, patchData)
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    isModified = true
                    Toast.makeText(this@SearchDetailActivity, "노트가 수정되었습니다.", Toast.LENGTH_SHORT).show()

                    // 마크다운 결과 반영
                    binding.tvTitle.text = newTitle
                    val markwon = Markwon.create(this@SearchDetailActivity)
                    markwon.setMarkdown(binding.tvContent, newContent)

                    // 수정 UI 비활성화
                    binding.modeExplain.visibility = View.GONE
                    binding.tvTitle.visibility = View.VISIBLE
                    binding.etTitleEditor.visibility = View.GONE
                    binding.tvContent.visibility = View.VISIBLE
                    binding.etContentEditor.visibility = View.GONE

                    // 숨겼던 뷰 복원
                    binding.bookmarkContainer.visibility = View.VISIBLE
                    binding.reactionLayout.visibility = View.VISIBLE
                    binding.tvRelatedTitle.visibility = View.VISIBLE
                    binding.relatedArticlesViewPager.visibility = View.VISIBLE
                    binding.dotsIndicator.visibility = View.VISIBLE

                    isEditMode = false
                } else {
                    Toast.makeText(this@SearchDetailActivity, "수정 실패", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("SaveNote", "오류", e)
                Toast.makeText(this@SearchDetailActivity, "네트워크 오류", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun deleteNote(noteId: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.noteApi.deleteNote(noteId)
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    Toast.makeText(this@SearchDetailActivity, "노트가 삭제되었습니다.", Toast.LENGTH_SHORT).show()

                    val result = Intent().apply {
                        putExtra("noteDeleted", true)
                    }
                    setResult(RESULT_OK, result)
                    finish()
                } else {
                    Toast.makeText(this@SearchDetailActivity, "삭제 실패: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("DeleteNote", "삭제 오류", e)
                Toast.makeText(this@SearchDetailActivity, "서버 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBookmarkButton() {
        val bookmarkContainer = binding.bookmarkContainer
        val bookmarkIcon = binding.btnBookmark
        val countTextView = binding.bookmarkCount

        // 초기 카운트 표시
        countTextView.text = bookmarkCount.toString()

        // 북마크 컨테이너 클릭 이벤트
        bookmarkContainer.setOnClickListener {
            // 북마크 상태 토글
            isBookmarked = !isBookmarked

            // 카운트 증가/감소 및 업데이트
            if (isBookmarked) {
                // 북마크 활성화 시 카운트 증가
                bookmarkCount++
                bookmarkIcon.setImageResource(R.drawable.ic_bookmark_filled)
            } else {
                // 북마크 비활성화 시 카운트 감소
                bookmarkCount--
                bookmarkIcon.setImageResource(R.drawable.ic_bookmark)
            }

            countTextView.text = bookmarkCount.toString()
        }
    }

    private fun setupReactionButtons() {

        setupEmojiToggle(binding.likeContainer, "LIKE")
        setupEmojiToggle(binding.heartContainer, "THUMBS_UP")
        setupEmojiToggle(binding.thinkingContainer, "THINKING")
        setupEmojiToggle(binding.fireContainer, "FIRE")
        setupEmojiToggle(binding.hundredContainer, "HUNDRED")
    }

    private fun setupEmojiToggle(container: View, emojiType: String) {
        container.setOnClickListener {
            val noteId = currentNoteId ?: return@setOnClickListener
            val currentNote = noteViewModel.selectedNote.value ?: return@setOnClickListener

            // 서버 enum 값이 대문자로 기대되므로 매핑을 정확히 맞춰야 함
            val emojiKeyMap = mapOf(
                "LIKE" to "like",
                "THUMBS_UP" to "thumbsUp",
                "THINKING" to "thinking",
                "FIRE" to "fire",
                "HUNDRED" to "hundred"
            )

            val clickedKey = emojiKeyMap[emojiType] ?: return@setOnClickListener
            val isClicked = currentNote.emojiStatus.clicked[clickedKey] ?: false

            // ViewModel 호출 (isClicked는 Boolean 확정됨)
            noteViewModel.toggleEmoji(noteId, emojiType, isClicked)
        }
    }

    private fun setupDotIndicators(size: Int) {
        // 기존 도트 제거
        binding.dotsIndicator.removeAllViews()

        // 도트 추가
        dotsIndicator = List(size) { index ->
            ImageView(this).apply {
                setImageResource(
                    if (index == 0) R.drawable.dot_selected
                    else R.drawable.dot_unselected
                )
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(8, 0, 8, 0)
                layoutParams = params
                binding.dotsIndicator.addView(this)
            }
        }
    }

    private fun updateDots(position: Int) {
        dotsIndicator.forEachIndexed { index, dot ->
            dot.setImageResource(
                if (index == position) R.drawable.dot_selected
                else R.drawable.dot_unselected
            )
        }
    }

    private fun showCommentBottomSheet() {
        // BottomSheetDialog 생성
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val commentView = layoutInflater.inflate(R.layout.layout_comment_bottom_sheet, null)
        bottomSheetDialog.setContentView(commentView)

        // 로그 추가 - 디버깅용
        Log.d("SearchDetailActivity", "Comments count: ${commentList.size}")

        // 댓글 목록이 비어있을 때 표시할 View
        val noCommentsView = commentView.findViewById<TextView>(R.id.tv_no_comments)

        // RecyclerView 설정
        val recyclerView = commentView.findViewById<RecyclerView>(R.id.rv_comments)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 확인용 로그
        Log.d("SearchDetailActivity", "RecyclerView visibility: ${recyclerView.visibility}")

        // 어댑터 설정
        val adapter = CommentAdapter(commentList)
        recyclerView.adapter = adapter

        // 댓글 수 설정
        val commentCountView = commentView.findViewById<TextView>(R.id.tv_comment_count)
        commentCountView.text = commentList.size.toString()

        // 댓글 목록이 비어있는지 확인하고 적절한 View 표시
        if (commentList.isEmpty()) {
            recyclerView.visibility = View.GONE
            noCommentsView.visibility = View.VISIBLE
            Log.d("SearchDetailActivity", "Comments list is empty, showing noCommentsView")
        } else {
            recyclerView.visibility = View.VISIBLE
            noCommentsView.visibility = View.GONE
            Log.d("SearchDetailActivity", "Showing comments in RecyclerView")
        }

        // 댓글 입력 버튼 이벤트
        val sendButton = commentView.findViewById<ImageButton>(R.id.btn_send_comment)
        val etComment = commentView.findViewById<EditText>(R.id.et_comment)

        sendButton.setOnClickListener {
            val commentText = etComment.text.toString().trim()
            if (commentText.isNotEmpty()) {
                // 새 댓글 추가
                val newComment = Comment("나", "방금", commentText, 0)
                commentList.add(0, newComment)

                Log.d("SearchDetailActivity", "Added new comment: $commentText")
                Log.d("SearchDetailActivity", "New comments count: ${commentList.size}")

                // 어댑터 업데이트
                adapter.notifyItemInserted(0)
                recyclerView.scrollToPosition(0)

                // 댓글 수 업데이트
                commentCountView.text = commentList.size.toString()

                // 입력창 비우기
                etComment.text.clear()

                // 댓글 목록이 이제 비어있지 않으므로 no comments 뷰 숨기기
                if (noCommentsView.visibility == View.VISIBLE) {
                    noCommentsView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    Log.d("SearchDetailActivity", "Hiding noCommentsView, showing RecyclerView")
                }

//                // 토스트 메시지로 댓글 추가 알림
//                Toast.makeText(this, "댓글이 추가되었습니다", Toast.LENGTH_SHORT).show()
            }
        }

        // 키보드에서 전송 버튼 클릭 시 댓글 전송
        etComment.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendButton.performClick()
                return@setOnEditorActionListener true
            }
            false
        }

        // 댓글이 있는 경우 BottomSheet의 높이 설정
        if (commentList.size > 0) {
            val params = recyclerView.layoutParams
            params.height = resources.displayMetrics.heightPixels / 2
            recyclerView.layoutParams = params
            Log.d("SearchDetailActivity", "Set RecyclerView height to half screen")
        }

        // BottomSheet 동작 설정
        val behavior = bottomSheetDialog.behavior
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true // 중간 상태 스킵

        // BottomSheet 닫기 설정
        // 배경 클릭 시 닫기
        bottomSheetDialog.setCancelable(true)
        bottomSheetDialog.setCanceledOnTouchOutside(true)

        // BottomSheet 표시
        bottomSheetDialog.show()
    }

    override fun onBackPressed() {
        val result = Intent().apply {
            putExtra("noteModified", true)
        }
        setResult(RESULT_OK, result)
        super.onBackPressed()
    }

}
