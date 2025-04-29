package com.example.tokkit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityQuizResultBinding

class QuizResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuizResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 인텐트에서 점수 정보 가져오기
        val score = intent.getIntExtra("SCORE", 0)
        val totalQuestions = intent.getIntExtra("TOTAL_QUESTIONS", 0)
        val userName = intent.getStringExtra("USER_NAME") ?: "Gomoph"

        // UI 업데이트
        binding.tvUserName.text = userName
        binding.tvScoreLabel.text = "님의 점수는"
        binding.tvScore.text = score.toString()
        binding.tvTotalScore.text = "/${totalQuestions * 8}" // 각 문제당 8점으로 계산

        // 점수에 따라 글자 색상 변경
        val scorePercentage = score.toFloat() / (totalQuestions * 8).toFloat()
        when {
            scorePercentage >= 0.8 -> binding.tvScore.setTextColor(resources.getColor(R.color.main, null)) // 우수
            scorePercentage >= 0.6 -> binding.tvScore.setTextColor(resources.getColor(R.color.chunglok, null)) // 보통
            else -> binding.tvScore.setTextColor(resources.getColor(android.R.color.holo_red_light, null)) // 미흡
        }

        // 확인 버튼 클릭 리스너
        binding.btnConfirm.setOnClickListener {
            finish()
        }
    }
}