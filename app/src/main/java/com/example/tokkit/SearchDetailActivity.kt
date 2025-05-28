package com.example.tokkit

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.Button
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
import com.example.tokkit.adapter.OnCommentLongClickListener
import com.example.tokkit.adapter.OnLikeClickListener
import com.example.tokkit.adapter.OnReplyClickListener
import com.example.tokkit.adapter.SimilarNoteAdapter
import com.example.tokkit.data.remote.model.BookmarkStatus
import com.example.tokkit.data.remote.model.Comment
import com.example.tokkit.data.remote.model.Note
import com.example.tokkit.databinding.ActivitySearchDetailBinding
import com.example.tokkit.util.RetrofitClient
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import com.example.tokkit.util.CustomToastUtil
import android.media.MediaPlayer

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

    private var currentPage = 0
    private val allComments = mutableListOf<Comment>()
    private var replyingToCommentId: Long? = null

    private var etComment: EditText? = null
    private var bottomSheetDialog: BottomSheetDialog? = null

    private var mediaPlayer: MediaPlayer? = null
    private var isAudioLoaded = false
    private var audioUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val noteId = intent.getStringExtra("NOTE_ID") ?: return
        currentNoteId = noteId

        // 음성파일 미리 불러오기
        fetchLectureAudio(noteId)

        binding.btnPlay.setOnClickListener {
            if (!isAudioLoaded || audioUrl == null) {
                CustomToastUtil.showToast(this, "음성 파일이 아직 준비되지 않았습니다.", R.drawable.ic_bot)
                return@setOnClickListener
            }

            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(audioUrl)
                    prepare()
                    setOnCompletionListener {
                        stopAudio()
                    }
                }
            }

            mediaPlayer?.start()

            binding.btnPlay.visibility = View.GONE
            binding.btnStop.visibility = View.VISIBLE
            binding.btnRestart.visibility = View.VISIBLE
        }



        binding.btnStop.setOnClickListener {
            mediaPlayer?.pause()
            binding.btnPlay.visibility = View.VISIBLE
            binding.btnStop.visibility = View.GONE
            binding.btnRestart.visibility = View.GONE
        }

        binding.btnRestart.setOnClickListener {
            if (!isAudioLoaded || audioUrl == null) {
                CustomToastUtil.showToast(this, "음성 파일이 아직 준비되지 않았습니다.", R.drawable.ic_bot)
                return@setOnClickListener
            }

            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(audioUrl)
                    setOnPreparedListener {
                        seekTo(0) // 처음으로 이동
                    }
                    setOnCompletionListener {
                        stopAudio()
                    }
                    prepareAsync() // 자동 재생 방지
                }
            } else {
                mediaPlayer?.pause()         // 혹시 재생 중이었으면 정지
                mediaPlayer?.seekTo(0)       // 처음으로 이동
            }

            // 버튼 상태 전환
            binding.btnRestart.visibility = View.GONE
            binding.btnStop.visibility = View.GONE
            binding.btnPlay.visibility = View.VISIBLE
        }


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
                val markwon = Markwon.builder(this)
                    .usePlugin(TablePlugin.create(this))
                    .build()
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
                CustomToastUtil.showToast(
                    context = this,
                    message = "노트를 불러올 수 없습니다.",
                    iconResId = R.drawable.ic_bot
                )
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
                // 태그 검색 상태가 있었는지 확인 데이터
                putExtra("wasTagSearch", intent.getBooleanExtra("isTagSearch", false))
                putExtra("tagName", intent.getStringExtra("tagName"))
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
                    CustomToastUtil.showToast(
                        context = this@SearchDetailActivity,
                        message = "노트가 수정되었습니다.",
                        iconResId = R.drawable.ic_bot
                    )

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
                    CustomToastUtil.showToast(
                        context = this@SearchDetailActivity,
                        message = "수정 실패",
                        iconResId = R.drawable.ic_bot
                    )
                }
            } catch (e: Exception) {
                Log.e("SaveNote", "오류", e)
                CustomToastUtil.showToast(
                    context = this@SearchDetailActivity,
                    message = "네트워크 오류",
                    iconResId = R.drawable.ic_bot
                )
            }
        }
    }


    private fun deleteNote(noteId: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.noteApi.deleteNote(noteId)
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    CustomToastUtil.showToast(
                        context = this@SearchDetailActivity,
                        message = "노트가 삭제되었습니다.",
                        iconResId = R.drawable.ic_bot
                    )
                    val result = Intent().apply {
                        putExtra("noteDeleted", true)
                    }
                    setResult(RESULT_OK, result)
                    finish()
                } else {
                    CustomToastUtil.showToast(
                        context = this@SearchDetailActivity,
                        message = "삭제 실패: ${response.body()?.message}",
                        iconResId = R.drawable.ic_bot
                    )
                }
            } catch (e: Exception) {
                Log.e("DeleteNote", "삭제 오류", e)
                CustomToastUtil.showToast(
                    context = this@SearchDetailActivity,
                    message = "서버 오류가 발생했습니다.",
                    iconResId = R.drawable.ic_bot
                )
            }
        }
    }

    private fun setupBookmarkButton() {
        val bookmarkContainer = binding.bookmarkContainer

        bookmarkContainer.setOnClickListener {
            val noteId = currentNoteId ?: return@setOnClickListener
            val currentNote = noteViewModel.selectedNote.value ?: return@setOnClickListener
            val isCurrentlyBookmarked = currentNote.bookmarkStatus?.clicked ?: false

            // 서버 요청
            noteViewModel.toggleBookmark(noteId, isCurrentlyBookmarked)

            // 북마크 상태 변경 시 결과 설정 (isBookmarkChanged를 추가)
            val result = Intent().apply {
                putExtra("isBookmarkChanged", true)
            }
            setResult(RESULT_OK, result)
        }

        // UI 반영
        noteViewModel.selectedNote.observe(this) { note ->
            note?.bookmarkStatus?.let { status ->
                isBookmarked = status.clicked
                bookmarkCount = status.count

                binding.bookmarkCount.text = bookmarkCount.toString()
                binding.btnBookmark.setImageResource(
                    if (isBookmarked) R.drawable.ic_bookmark_filled
                    else R.drawable.ic_bookmark
                )
            }
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
        val noteId = currentNoteId ?: return

        // 댓글 초기화
        noteViewModel.resetComments()

        // BottomSheetDialog 생성
        bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val commentView = layoutInflater.inflate(R.layout.layout_comment_bottom_sheet, null)
        bottomSheetDialog?.setContentView(commentView)

        val sendButton = commentView.findViewById<ImageButton>(R.id.btn_send_comment)
        etComment = commentView.findViewById(R.id.et_comment)

        // 댓글 목록이 비어있을 때 표시할 View
        val noCommentsView = commentView.findViewById<TextView>(R.id.tv_no_comments)

        // RecyclerView 설정
        val recyclerView = commentView.findViewById<RecyclerView>(R.id.rv_comments)

        val adapter = CommentAdapter(
            rootComments = mutableListOf(),
            onLongClickListener = object : OnCommentLongClickListener {
                override fun onLongClick(view: View, comment: Comment) {
                    showCommentPopup(view, comment)
                }
            },
            onLikeClickListener = object : OnLikeClickListener {
                override fun onClick(comment: Comment) {
                    toggleEmoji(comment.commentId, "LIKE", comment.isLiked)
                }
            },
            onReplyClickListener = object : OnReplyClickListener {
                override fun onClick(parentComment: Comment) {
                    replyingToCommentId = parentComment.commentId
                    etComment?.requestFocus()
                    etComment?.hint = "답글 작성 중..." // EditText 힌트 변경
                }
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val commentCountView = commentView.findViewById<TextView>(R.id.tv_comment_count)

        // 댓글 observe
        noteViewModel.comments.observe(this) { commentResponses ->
            Log.d("CommentDebug", "원본 댓글 응답: $commentResponses")

            val comments = commentResponses.map { response ->
                val createdTime = parseTimeAgo(response.createdAt)
                Comment(
                    username = response.writer,
                    time = createdTime,
                    content = response.content,
                    likeCount = response.emojis["LIKE"]?.count ?: 0,
                    commentId = response.commentId,
                    isMine = response.isMine,
                    isLiked = response.emojis["LIKE"]?.reactedByCurrentUser ?: false,
                    parentId = response.parentId,
                    profileImageUrl = response.profileImageUrl // ✅ 이 줄 추가!
                )
            }

            if (currentPage == 0) allComments.clear()
            allComments.addAll(comments)

            Log.d("CommentDebug", "댓글 목록: ${comments.map { "${it.commentId}(부모: ${it.parentId})" }}")

            // 중복 제거 후 시간순 정렬
            val distinctComments = allComments.distinctBy { it.commentId }
                .sortedBy { it.commentId }

            // 계층 구조로 변환
            val rootComments = organizeCommentsHierarchy(distinctComments)
            Log.d("CommentDebug", "계층 구조 변환 후 루트 댓글: ${rootComments.size}")

            // 비어있는지 확인
            if (rootComments.isEmpty()) {
                noCommentsView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                noCommentsView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                adapter.updateComments(rootComments)
            }
        }

        // 댓글 수 observe
        noteViewModel.totalCommentCount.observe(this) { count ->
            commentCountView.text = count.toString()
        }

        // 초기 댓글 로드
        noteViewModel.loadComments(noteId, page = 0)

        // 페이징 스크롤
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val totalItemCount = layoutManager.itemCount

                if (lastVisible + 2 >= totalItemCount && noteViewModel.isLastCommentPage.value != true) {
                    currentPage++
                    noteViewModel.loadComments(noteId, page = currentPage)
                }
            }
        })

        // 댓글 작성
        sendButton.setOnClickListener {
            val text = etComment?.text.toString().trim()
            if (text.isNotEmpty()) {
                noteViewModel.postComment(
                    noteId = noteId,
                    content = text,
                    parentId = replyingToCommentId,
                    onSuccess = {
                        etComment?.text?.clear()
                        replyingToCommentId = null
                        etComment?.hint = "댓글을 입력하세요"
//                        Toast.makeText(this, "댓글 등록 완료", Toast.LENGTH_SHORT).show()

                        // 댓글 등록 후 초기화 + 0페이지 로드
                        currentPage = 0
                        allComments.clear()
                        noteViewModel.resetComments()
                        noteViewModel.loadComments(noteId, 0)
                    },
                    onFail = {
                        CustomToastUtil.showToast(
                            context = this,
                            message = "댓글 등록 실패",
                            iconResId = R.drawable.ic_bot
                        )
                    }
                )
            }
        }

        // 키보드에서 전송 버튼 클릭 시 댓글 전송
        etComment?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendButton.performClick()
                return@setOnEditorActionListener true
            }
            false
        }

        val params = recyclerView.layoutParams
        params.height = resources.displayMetrics.heightPixels / 2
        recyclerView.layoutParams = params

        // BottomSheet 동작 설정
        val behavior = bottomSheetDialog?.behavior
        behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        behavior?.skipCollapsed = true // 중간 상태 스킵

        // BottomSheet 닫기 설정
        // 배경 클릭 시 닫기
        bottomSheetDialog?.setCancelable(true)
        bottomSheetDialog?.setCanceledOnTouchOutside(true)

        // BottomSheet 표시
        bottomSheetDialog?.show()
    }

    // 수정된 organizeCommentsHierarchy 함수 - 다중 레벨 대댓글 완전 지원
    private fun organizeCommentsHierarchy(flatComments: List<Comment>): List<Comment> {
        val rootComments = mutableListOf<Comment>()
        val commentMap = mutableMapOf<Long, Comment>()

        Log.d("CommentDebug", "계층 구조 변환 시작 - 입력 댓글 수: ${flatComments.size}")

        // 1단계: 모든 댓글을 ID로 맵핑하고 replies 초기화
        flatComments.forEach { comment ->
            val commentWithReplies = comment.copy(replies = mutableListOf())
            commentMap[comment.commentId] = commentWithReplies
            Log.d("CommentDebug", "댓글 맵핑: ID=${comment.commentId}, parentId=${comment.parentId}")
        }

        // 2단계: 부모-자식 관계 설정
        flatComments.forEach { comment ->
            val parentId = comment.parentId
            if (parentId == null) {
                // parentId가 null이면 루트 댓글
                commentMap[comment.commentId]?.let { rootComments.add(it) }
                Log.d("CommentDebug", "루트 댓글 추가: ID=${comment.commentId}")
            } else {
                // parentId가 있으면 해당 부모의 대댓글
                val parentComment = commentMap[parentId]
                val childComment = commentMap[comment.commentId]
                if (parentComment != null && childComment != null) {
                    parentComment.replies.add(childComment)
                    Log.d("CommentDebug", "대댓글 추가: ID=${comment.commentId} -> 부모=${parentId}")
                } else {
                    Log.e("CommentDebug", "부모 댓글을 찾을 수 없음: parentId=$parentId")
                    // 부모를 찾을 수 없으면 루트 댓글로 처리
                    commentMap[comment.commentId]?.let { rootComments.add(it) }
                    Log.d("CommentDebug", "부모를 찾을 수 없어 루트 댓글로 처리: ID=${comment.commentId}")
                }
            }
        }

        Log.d("CommentDebug", "계층 구조 변환 완료 - 루트 댓글 수: ${rootComments.size}")
        rootComments.forEach { root ->
            Log.d("CommentDebug", "루트 댓글: ID=${root.commentId}")
            logRepliesRecursively(root.replies, 1)
        }

        return rootComments
    }

    // 대댓글을 재귀적으로 로깅하는 함수
    private fun logRepliesRecursively(replies: List<Comment>, depth: Int) {
        replies.forEach { reply ->
            val indent = "  ".repeat(depth)
            Log.d("CommentDebug", "$indent- 대댓글: ID=${reply.commentId}, 하위 댓글 수=${reply.replies.size}")
            if (reply.replies.isNotEmpty()) {
                logRepliesRecursively(reply.replies, depth + 1)
            }
        }
    }

    private fun parseTimeAgo(createdAt: String): String {
        return try {
            val formatter = DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 6, true)
                .toFormatter()

            val parsedTime = LocalDateTime.parse(createdAt, formatter)
            val now = LocalDateTime.now()
            val duration = Duration.between(parsedTime, now)

            when {
                duration.toMinutes() < 1 -> "방금"
                duration.toHours() < 1 -> "${duration.toMinutes()}분 전"
                duration.toHours() < 24 -> "${duration.toHours()}시간 전"
                else -> "${duration.toDays()}일 전"
            }
        } catch (e: Exception) {
            Log.e("TimeParser", "createdAt 파싱 실패: $createdAt", e)
            "방금"
        }
    }

    private fun showCommentPopup(anchorView: View, comment: Comment) {

        val popup = PopupMenu(this, anchorView, Gravity.END)

        popup.menu.add(if (comment.isLiked) "좋아요 취소" else "좋아요")
        popup.menu.add("답글 달기")

        if (comment.isMine) {
            popup.menu.add("수정")
            popup.menu.add("삭제")
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "좋아요" -> {
                    toggleEmoji(comment.commentId, "LIKE", false)
                    true
                }
                "좋아요 취소" -> {
                    toggleEmoji(comment.commentId, "LIKE", true)
                    true
                }
                "답글 달기" -> {
                    replyingToCommentId = comment.commentId
                    etComment?.requestFocus()
                    etComment?.hint = "답글 작성 중..."
                    // 키보드 자동 열기 (선택적)
                    etComment?.post {
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                        imm.showSoftInput(etComment, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                    }
                    bottomSheetDialog?.show()
                    true
                }
                "수정" -> {
                    showEditCommentDialog(comment)
                    true
                }
                "삭제" -> {
                    noteViewModel.deleteComment(comment.commentId) { success ->
                        if (success) {
                            // 댓글 리스트 갱신
                            currentPage = 0
                            allComments.clear()
                            noteViewModel.resetComments()
                            noteViewModel.loadComments(currentNoteId!!, 0)
                        } else {
                            CustomToastUtil.showToast(
                                context = this,
                                message = "삭제 실패",
                                iconResId = R.drawable.ic_bot
                            )
                        }
                    }
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    private fun showEditCommentDialog(comment: Comment) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_comment)

        // 배경 투명하게 지정 (중요)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // 내부 View 처리
        val editText = dialog.findViewById<EditText>(R.id.edit_comment)
        val btnCancel = dialog.findViewById<Button>(R.id.btn_cancel)
        val btnSave = dialog.findViewById<Button>(R.id.btn_save)

        editText.setText(comment.content)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val newContent = editText.text.toString().trim()
            if (newContent.isNotBlank()) {
                noteViewModel.updateComment(comment.commentId, newContent) { success ->
                    if (success) {
                        currentPage = 0
                        allComments.clear()
                        noteViewModel.resetComments()
                        noteViewModel.loadComments(currentNoteId!!, 0)
                    } else {
                        CustomToastUtil.showToast(
                            context = this,
                            message = "수정 실패",
                            iconResId = R.drawable.ic_bot
                        )
                    }
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun toggleEmoji(commentId: Long, emojiName: String, isAlreadyReacted: Boolean) {
        noteViewModel.toggleCommentEmoji(
            commentId = commentId,
            emojiName = emojiName,
            isAlreadyReacted = isAlreadyReacted
        ) { success ->
            if (success) {
                currentPage = 0
                allComments.clear()
                noteViewModel.resetComments()
                noteViewModel.loadComments(currentNoteId!!, 0)
            } else {
                CustomToastUtil.showToast(
                    context = this,
                    message = "이모지 처리 실패",
                    iconResId = R.drawable.ic_bot
                )
            }
        }
    }

    override fun onBackPressed() {
        val result = Intent().apply {
            putExtra("noteModified", true)
            // 태그 검색 상태가 있었는지 확인 데이터
            putExtra("wasTagSearch", intent.getBooleanExtra("isTagSearch", false))
            putExtra("tagName", intent.getStringExtra("tagName"))
        }
        setResult(RESULT_OK, result)
        super.onBackPressed()
    }

    private fun fetchLectureAudio(noteId: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.noteApi.getLectureAudio(noteId)

                Log.d("AudioFetch", "HTTP 상태 코드: ${response.code()}")
                Log.d("AudioFetch", "응답 바디: ${response.body()}")
                Log.d("AudioFetch", "에러 바디: ${response.errorBody()?.string()}")

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    audioUrl = response.body()!!.result.audioUrl
                    isAudioLoaded = true
                    Log.d("AudioFetch", "오디오 URL: $audioUrl")
                } else {
                    Log.w("AudioFetch", "오디오 응답 실패. isSuccess=${response.body()?.isSuccess}, message=${response.body()?.message}")
                    CustomToastUtil.showToast(this@SearchDetailActivity, "음성파일을 불러오지 못했습니다.", R.drawable.ic_bot)
                }
            } catch (e: Exception) {
                Log.e("AudioFetch", "예외 발생: ${e.localizedMessage}", e)
                CustomToastUtil.showToast(this@SearchDetailActivity, "네트워크 오류", R.drawable.ic_bot)
            }
        }
    }


    private fun stopAudio() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        binding.btnPlay.visibility = View.VISIBLE
        binding.btnStop.visibility = View.GONE
        binding.btnRestart.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudio()
    }

}
