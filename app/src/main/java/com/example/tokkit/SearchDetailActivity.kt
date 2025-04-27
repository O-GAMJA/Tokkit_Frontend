package com.example.tokkit

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.tokkit.databinding.ActivitySearchDetailBinding
import com.example.tokkit.model.Article

class SearchDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchDetailBinding
    private var bookmarkCount = 3 // 초기 북마크 카운트
    private var isBookmarked = false // 북마크 상태

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
    }

    private fun setupBookmarkButton() {
        val bookmarkContainer = findViewById<FrameLayout>(R.id.bookmarkContainer)
        val bookmarkIcon = findViewById<ImageView>(R.id.btnBookmark)
        val countTextView = findViewById<TextView>(R.id.bookmarkCount)

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
        // 댓글 버튼
        binding.commentButton.setOnClickListener {
            // 댓글 화면으로 이동 로직
        }

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

        // 박수 버튼
        binding.clapContainer.setOnClickListener {
            // 박수 카운트 증가 로직
            val currentCount = binding.clapCount.text.toString().toInt()
            binding.clapCount.text = (currentCount + 1).toString()
        }

        // 감사 버튼
        binding.thanksContainer.setOnClickListener {
            // 감사 카운트 증가 로직
            val currentCount = binding.thanksCount.text.toString().toInt()
            binding.thanksCount.text = (currentCount + 1).toString()
        }
    }

    companion object {
        const val EXTRA_ARTICLE = "EXTRA_ARTICLE"
    }
}