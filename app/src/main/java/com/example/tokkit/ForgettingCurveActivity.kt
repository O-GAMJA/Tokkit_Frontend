package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.data.remote.model.ReviewStat
import com.example.tokkit.data.remote.repository.ReviewRepository
import com.example.tokkit.databinding.ActivityForgettingCurveBinding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokkit.adapter.ReviewStatAdapter
import kotlinx.coroutines.launch

class ForgettingCurveActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgettingCurveBinding
    private var articleTitle: String? = null
    private var articleStage: Int = 0

    private var noteId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgettingCurveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 인텐트에서 아티클 정보 받기
        articleTitle = intent.getStringExtra("ARTICLE_TITLE")
        articleStage = intent.getIntExtra("ARTICLE_STAGE", 0)

        // 제목 설정
        binding.tvTitle.text = "복습 ${articleStage}단계"

        setupListeners()

        noteId = intent.getStringExtra("NOTE_ID")

        noteId?.let { loadReviewDetail(it) }
    }

    private fun loadReviewDetail(noteId: String) {
        lifecycleScope.launch {
            val result = ReviewRepository().getReviewDetail(noteId)
            if (result != null) {
                updateReviewTable(result.reviewStats)
                //updateGraph(result.reviewCurvePoints)
            } else {
                Toast.makeText(this@ForgettingCurveActivity, "복습 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateReviewTable(stats: List<ReviewStat>) {
        binding.recyclerReviewStats.layoutManager = LinearLayoutManager(this)
        binding.recyclerReviewStats.adapter = ReviewStatAdapter(stats)

        // 회색 선 View의 visibility 조정
        binding.viewDivider.visibility = if (stats.isNotEmpty()) View.VISIBLE else View.GONE
    }


    private fun setupListeners() {
        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 퀴즈 버튼
        binding.btnQuiz.setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java)
            intent.putExtra("ARTICLE_TITLE", articleTitle)
            intent.putExtra("ARTICLE_STAGE", articleStage)
            startActivity(intent)
        }

        // 말하기 버튼
        binding.btnSpeak.setOnClickListener {
        }

        // 노트 보기 버튼
        binding.btnNote.setOnClickListener {
        }
    }
}