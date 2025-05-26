package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.data.remote.model.ReviewStat
import com.example.tokkit.data.remote.repository.ReviewRepository
import com.example.tokkit.databinding.ActivityForgettingCurveBinding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokkit.adapter.ReviewStatAdapter
import kotlinx.coroutines.launch
import android.util.Log
import java.nio.file.Paths

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
                updateGraph(articleStage)
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

    private fun updateGraph(stage: Int) {
        val imageResId = when (stage) {
            0 -> R.drawable.ic_ebbing0
            1 -> R.drawable.ic_ebbing1
            2 -> R.drawable.ic_ebbing2
            3 -> R.drawable.ic_ebbing3
            4 -> R.drawable.ic_ebbing4
            else -> R.drawable.ic_ebbing0 // 기본 그래프
        }

        binding.cardGraph.findViewById<ImageView>(R.id.ivEbbing).setImageResource(imageResId)
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
            val noteContent = intent.getStringExtra("NOTE_CONTENT") ?: return@setOnClickListener
            Log.d("DEBUG", "NOTE_CONTENT = $noteContent")

            val socModel = android.os.Build.SOC_MODEL
            val htpConfig = when (socModel) {
                "SM8750" -> "qualcomm-snapdragon-8-elite.json"
                "SM8650" -> "qualcomm-snapdragon-8-gen3.json"
                "QCS8550" -> "qualcomm-snapdragon-8-gen2.json"
                else -> {
                    Toast.makeText(this, "지원되지 않는 디바이스입니다", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
            }

            val htpConfigPath = Paths.get(externalCacheDir!!.absolutePath, "htp_config", htpConfig).toString()
            val modelName = "llama3_2_3b"

            val intent = Intent(this, ReviewSpeakingActivity::class.java).apply {
                putExtra(ReviewSpeakingActivity.EXTRA_NOTE_CONTENT, noteContent)
                putExtra(ReviewSpeakingActivity.KEY_HTP_CONFIG, htpConfigPath)
                putExtra(ReviewSpeakingActivity.KEY_MODEL_NAME, modelName)
            }

            startActivity(intent)
        }



        // 노트 보기 버튼
        binding.btnNote.setOnClickListener {
            val intent = Intent(this, SearchDetailActivity::class.java)

            intent.putExtra("NOTE_ID", noteId)

            // 기본값: 태그 검색 아님
            intent.putExtra("isTagSearch", false)
            intent.putExtra("tagName", "")

            startActivity(intent)
        }
    }
}