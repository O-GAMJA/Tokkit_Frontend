package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityForgettingCurveBinding

class ForgettingCurveActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgettingCurveBinding
    private var articleTitle: String? = null
    private var articleStage: Int = 0

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