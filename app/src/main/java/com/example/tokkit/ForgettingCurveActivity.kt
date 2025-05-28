package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
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
import com.example.tokkit.util.CustomToastUtil

class ForgettingCurveActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgettingCurveBinding
    private var articleTitle: String? = null
    private var articleStage: Int = 0
    private var noteId: String? = null
    private var isComplete: Boolean = false // 완료 상태 플래그

    companion object {
        private const val REQUEST_REVIEW_SPEAKING = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgettingCurveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 인텐트에서 아티클 정보 받기
        articleTitle = intent.getStringExtra("ARTICLE_TITLE")
        articleStage = intent.getIntExtra("ARTICLE_STAGE", 0)
        noteId = intent.getStringExtra("NOTE_ID")
        isComplete = intent.getBooleanExtra("IS_COMPLETE", false) // 완료 상태 받기

        // 제목 설정
        updateTitle()

        setupListeners()
        noteId?.let { loadReviewDetail(it) }
    }

    private fun updateTitle() {
        binding.tvTitle.text = if (isComplete) {
            "복습 완료"
        } else {
            "복습 ${articleStage}단계"
        }
    }

    private fun setupListeners() {
        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnQuiz.setOnClickListener {
            if (isComplete) {
                CustomToastUtil.showToast(
                    context = this,
                    message = "이미 복습이 완료된 노트입니다.",
                    iconResId = R.drawable.ic_bot
                )
                return@setOnClickListener
            }

            val memberId = 1L
            if (noteId == null) return@setOnClickListener

            lifecycleScope.launch {
                val quizList = ReviewRepository().getQuizzes(memberId, noteId!!)
                if (quizList.isNotEmpty()) {
                    val intent = Intent(this@ForgettingCurveActivity, QuizActivity::class.java).apply {
                        putExtra("ARTICLE_TITLE", articleTitle)
                        putExtra("ARTICLE_STAGE", articleStage)
                        putExtra("NOTE_ID", noteId)
                        putParcelableArrayListExtra("QUIZ_LIST", ArrayList(quizList))
                    }
                    startActivity(intent)
                } else {
                    CustomToastUtil.showToast(
                        context = this@ForgettingCurveActivity,
                        message = "퀴즈가 존재하지 않습니다.",
                        iconResId = R.drawable.ic_bot
                    )
                }
            }
        }


        // 말하기 버튼 - startActivityForResult 사용
        binding.btnSpeak.setOnClickListener {
            if (isComplete) {
                CustomToastUtil.showToast(
                    context = this,
                    message = "이미 복습이 완료된 노트입니다.",
                    iconResId = R.drawable.ic_bot
                )
                return@setOnClickListener
            }

            val noteContent = intent.getStringExtra("NOTE_CONTENT") ?: return@setOnClickListener
            Log.d("DEBUG", "NOTE_CONTENT = $noteContent")

            val socModel = android.os.Build.SOC_MODEL
            val htpConfig = when (socModel) {
                "SM8750" -> "qualcomm-snapdragon-8-elite.json"
                "SM8650" -> "qualcomm-snapdragon-8-gen3.json"
                "QCS8550" -> "qualcomm-snapdragon-8-gen2.json"
                else -> {
                    CustomToastUtil.showToast(
                        context = this,
                        message = "지원되지 않는 디바이스입니다",
                        iconResId = R.drawable.ic_bot
                    )
                    return@setOnClickListener
                }
            }

            val htpConfigPath = Paths.get(externalCacheDir!!.absolutePath, "htp_config", htpConfig).toString()
            val modelName = "llama3_2_3b"

            val intent = Intent(this, ReviewSpeakingActivity::class.java).apply {
                putExtra(ReviewSpeakingActivity.EXTRA_NOTE_ID, noteId)
                putExtra(ReviewSpeakingActivity.EXTRA_NOTE_CONTENT, noteContent)
                putExtra(ReviewSpeakingActivity.KEY_HTP_CONFIG, htpConfigPath)
                putExtra(ReviewSpeakingActivity.KEY_MODEL_NAME, modelName)
            }

            // startActivityForResult 사용
            startActivityForResult(intent, REQUEST_REVIEW_SPEAKING)
        }

        // 노트 보기 버튼
        binding.btnNote.setOnClickListener {
            val intent = Intent(this, SearchDetailActivity::class.java)
            intent.putExtra("NOTE_ID", noteId)
            intent.putExtra("isTagSearch", false)
            intent.putExtra("tagName", "")
            startActivity(intent)
        }
    }

    // ActivityResult 처리
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_REVIEW_SPEAKING && resultCode == RESULT_OK) {
            // 복습이 성공적으로 완료되었을 때
            val newStage = data?.getStringExtra("NEW_STAGE")

            // 새 단계가 COMPLETE인지 확인
            if (newStage == "COMPLETE") {
                isComplete = true
                articleStage = 5 // 완료 상태의 경우 최대 단계로 설정
            } else {
                val newStageInt = data?.getIntExtra("NEW_STAGE_INT", articleStage) ?: articleStage
                articleStage = newStageInt
                isComplete = false
            }

            // 제목 업데이트
            updateTitle()

            // 데이터 다시 로드
            noteId?.let { loadReviewDetail(it) }

            // 그래프 업데이트
            updateGraph(articleStage, isComplete)

            val message = if (isComplete) {
                "축하합니다! 모든 복습이 완료되었습니다!"
            } else {
                "복습이 완료되었습니다! 새 단계: $newStage"
            }
            CustomToastUtil.showToast(
                context = this,
                message = message,
                iconResId = R.drawable.ic_bot
            )
        }
    }

    private fun loadReviewDetail(noteId: String) {
        lifecycleScope.launch {
            val result = ReviewRepository().getReviewDetail(noteId)
            if (result != null) {
                updateReviewTable(result.reviewStats)

                // currentStage 처리
                when (result.currentStage) {
                    "COMPLETE" -> {
                        isComplete = true
                        articleStage = 5 // 완료 상태에서는 최대 단계로 표시
                    }
                    else -> {
                        isComplete = false
                        val currentStageInt = result.currentStage.filter { it.isDigit() }.toIntOrNull() ?: 0
                        articleStage = currentStageInt
                    }
                }

                // 제목 업데이트
                updateTitle()
                updateGraph(articleStage, isComplete)

            } else {
                CustomToastUtil.showToast(
                    context = this@ForgettingCurveActivity,
                    message = "복습 정보를 불러오지 못했습니다.",
                    iconResId = R.drawable.ic_bot
                )
            }
        }
    }

    private fun updateReviewTable(stats: List<ReviewStat>) {
        binding.recyclerReviewStats.layoutManager = LinearLayoutManager(this)
        binding.recyclerReviewStats.adapter = ReviewStatAdapter(stats)
        binding.viewDivider.visibility = if (stats.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateGraph(stage: Int, isComplete: Boolean = false) {
        val imageResId = if (isComplete) {
            // 완료 상태일 때는 특별한 완료 그래프 또는 최종 단계 그래프
            R.drawable.ic_ebbing4
        } else {
            when (stage) {
                0 -> R.drawable.ic_ebbing0
                1 -> R.drawable.ic_ebbing1
                2 -> R.drawable.ic_ebbing2
                3 -> R.drawable.ic_ebbing3
                4 -> R.drawable.ic_ebbing4
                else -> R.drawable.ic_ebbing0
            }
        }
        binding.cardGraph.findViewById<ImageView>(R.id.ivEbbing).setImageResource(imageResId)
    }

    override fun onResume() {
        super.onResume()
        // onResume에서도 데이터 리로드
        noteId?.let { loadReviewDetail(it) }
    }
}