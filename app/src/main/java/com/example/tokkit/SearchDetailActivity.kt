package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.tokkit.adapter.CommentAdapter
import com.example.tokkit.adapter.RelatedArticlesAdapter
import com.example.tokkit.databinding.ActivitySearchDetailBinding
import com.example.tokkit.model.Article
import com.example.tokkit.model.Comment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class SearchDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchDetailBinding
    private var bookmarkCount = 3 // 초기 북마크 카운트
    private var isBookmarked = false // 북마크 상태
    private lateinit var dotsIndicator: List<ImageView>

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

        // 인텐트에서 데이터 가져오기
        val title = intent.getStringExtra("ARTICLE_TITLE") ?: ""
        val content = intent.getStringExtra("ARTICLE_CONTENT") ?: ""
        val imageResId = intent.getIntExtra("ARTICLE_IMAGE", R.drawable.ic_tcp_ip)

        // 뒤로가기 버튼 설정
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 북마크 버튼 설정
        setupBookmarkButton()

        // 데이터 표시
        binding.ivArticleImage.setImageResource(imageResId)
        binding.tvTitle.text = title
        binding.tvContent.text = content

        // 이모티콘 버튼 기능
        setupReactionButtons()

        // 연관 글 ViewPager 설정
        setupRelatedArticlesViewPager()

        // 댓글 버튼 클릭 이벤트 설정
        binding.commentButton.setOnClickListener {
            showCommentBottomSheet()
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
        // 좋아요 버튼
        binding.likeContainer.setOnClickListener {
            // 좋아요 카운트 증가 로직
            val currentCount = binding.likeCount.text.toString().toInt()
            binding.likeCount.text = (currentCount + 1).toString()
        }

        // 하트 버튼
        binding.heartContainer.setOnClickListener {
            // 하트 카운트 증가 로직
            val currentCount = binding.heartCount.text.toString().toInt()
            binding.heartCount.text = (currentCount + 1).toString()
        }

        // 궁금해요 버튼
        binding.thinkingContainer.setOnClickListener {
            // 궁금해요 카운트 증가 로직
            val currentCount = binding.thinkingCount.text.toString().toInt()
            binding.thinkingCount.text = (currentCount + 1).toString()
        }

        // 불 버튼
        binding.fireContainer.setOnClickListener {
            // 불 카운트 증가 로직
            val currentCount = binding.fireCount.text.toString().toInt()
            binding.fireCount.text = (currentCount + 1).toString()
        }

        // 100점 버튼
        binding.hundredContainer.setOnClickListener {
            // 100점 카운트 증가 로직
            val currentCount = binding.hundredCount.text.toString().toInt()
            binding.hundredCount.text = (currentCount + 1).toString()
        }
    }

    private fun setupRelatedArticlesViewPager() {
        // 샘플 연관 글 데이터
        val relatedArticles = listOf(
            Article(
                "네트워크 - OSI 7계층",
                "OSI 7계층은 네트워크 통신을 체계적으로 이해하고 설계할 수 있도록 국제표준화기구(ISO)에서 정의한 참조 모델입니다. 각 계층은 물리, 데이터링크, 네트워크, 전송, 세션, 표현, 응용 계층으로 구성되며, 각각의 계층은 특정한 기능을 수행하여 데이터가 송수신되는 과정을 단계적으로 처리합니다. 예를 들어, 물리 계층은 실제 전기적 신호의 전송을 담당하고, 전송 계층은 오류 복구 및 흐름 제어를 통해 안정적인 데이터 전송을 보장합니다. 이러한 계층 구조는 네트워크 장비와 소프트웨어가 상호 운용되기 쉽게 만들고, 문제를 특정 계층으로 국한하여 디버깅할 수 있게 도와줍니다.",
                "2024.01.05",
                R.drawable.ic_tcp_ip
            ),
            Article(
                "HTTP 프로토콜",
                "HTTP(HyperText Transfer Protocol)는 웹 브라우저와 웹 서버 간의 통신에 사용되는 대표적인 애플리케이션 계층 프로토콜입니다. 클라이언트는 HTTP 요청(request)을 통해 특정 웹 리소스(HTML, 이미지 등)를 서버에 요청하고, 서버는 이에 대한 HTTP 응답(response)을 반환합니다. HTTP는 기본적으로 텍스트 기반의 프로토콜이며, 상태를 저장하지 않는 비연결형(stateless) 프로토콜이지만, 쿠키나 세션을 활용하여 상태를 관리할 수 있습니다. 또한, HTTP/2와 HTTP/3 등 최신 버전에서는 성능 개선과 보안 강화가 이루어졌으며, HTTPS는 TLS 암호화를 통해 데이터 전송의 보안성을 보장합니다.",
                "2024.01.06",
                R.drawable.ic_tcp_ip
            ),
            Article(
                "네트워크 보안",
                "네트워크 보안은 외부 침입이나 내부 위협으로부터 네트워크 자원과 데이터를 보호하기 위한 기술과 정책의 집합입니다. 주요 목표는 기밀성(confidentiality), 무결성(integrity), 가용성(availability)을 보장하는 것이며, 이를 위해 방화벽, 침입 탐지 시스템(IDS), 가상 사설망(VPN), 암호화 기법 등이 사용됩니다. 특히, 사이버 공격의 형태가 점점 고도화됨에 따라, 네트워크 보안은 단순한 접근 통제를 넘어, 지속적인 모니터링과 위협 탐지, 사고 대응 체계를 갖추는 것이 중요해졌습니다. 기업과 기관은 보안 정책을 수립하고 정기적인 취약점 점검을 통해 보안 수준을 유지해야 합니다.",
                "2024.01.07",
                R.drawable.ic_tcp_ip
            ),
            Article(
                "라우팅 프로토콜",
                "라우팅 프로토콜은 네트워크 상의 라우터들이 서로 정보를 교환하며, 데이터 패킷을 가장 효율적으로 전달할 수 있는 경로를 결정하는 데 사용되는 규칙들의 집합입니다. 대표적인 라우팅 프로토콜에는 RIP(Routing Information Protocol), OSPF(Open Shortest Path First), BGP(Border Gateway Protocol) 등이 있으며, 이들은 거리 벡터(Distance Vector)나 링크 상태(Link State) 알고리즘을 기반으로 경로를 계산합니다. 라우팅 프로토콜은 네트워크의 구조 변화에 따라 자동으로 경로를 재조정하여 유연한 데이터 전달을 가능하게 하며, 특히 대규모 네트워크에서는 효율적인 라우팅이 네트워크 성능에 큰 영향을 미칩니다.",
                "2024.01.08",
                R.drawable.ic_tcp_ip
            )
        )

        // 어댑터 설정
        val adapter = RelatedArticlesAdapter(relatedArticles) { article ->
            // 아이템 클릭 시 해당 글로 이동
            val intent = Intent(this, SearchDetailActivity::class.java)
            intent.putExtra("ARTICLE_TITLE", article.title)
            intent.putExtra("ARTICLE_CONTENT", article.content)
            intent.putExtra("ARTICLE_IMAGE", article.imageResId)
            startActivity(intent)
        }

        binding.relatedArticlesViewPager.adapter = adapter
        binding.relatedArticlesViewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        // 페이지 변경 리스너 설정
        binding.relatedArticlesViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
            }
        })

        // 도트 인디케이터 초기화
        setupDotIndicators(relatedArticles.size)
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
    }}