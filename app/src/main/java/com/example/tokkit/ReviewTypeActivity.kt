package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tokkit.data.remote.repository.ReviewRepository
import com.example.tokkit.databinding.ActivityReviewTypeBinding
import kotlinx.coroutines.launch
import java.nio.file.Paths
import com.example.tokkit.data.remote.repository.NoteRepository


class ReviewTypeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewTypeBinding

    private var articleTitle: String? = null
    private var articleStage: Int = 0
    private var noteId: String? = null
    private var isComplete: Boolean = false // 완료 상태 플래그

    companion object {
        private const val REQUEST_REVIEW_SPEAKING = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewTypeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        noteId = intent.getStringExtra("NOTE_ID")

        binding.btnBack.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // 말하기 버튼
        binding.cardSpeak.setOnClickListener {
            Log.d("DEBUG", "cardSpeak 클릭됨")
            if (isComplete) {
                Toast.makeText(this, "이미 복습이 완료된 노트입니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (noteId == null) {
                Toast.makeText(this, "노트 ID가 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val note = NoteRepository().getNoteById(noteId!!)
                if (note == null || note.content == null) {
                    Toast.makeText(this@ReviewTypeActivity, "노트를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val noteContent = note.content
                Log.d("DEBUG", "불러온 NOTE_CONTENT = $noteContent")

                val socModel = android.os.Build.SOC_MODEL
                val htpConfig = when (socModel) {
                    "SM8750" -> "qualcomm-snapdragon-8-elite.json"
                    "SM8650" -> "qualcomm-snapdragon-8-gen3.json"
                    "QCS8550" -> "qualcomm-snapdragon-8-gen2.json"
                    else -> {
                        Toast.makeText(this@ReviewTypeActivity, "지원되지 않는 디바이스입니다", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                }

                val htpConfigPath =
                    Paths.get(externalCacheDir!!.absolutePath, "htp_config", htpConfig).toString()
                val modelName = "llama3_2_3b"

                val intent = Intent(this@ReviewTypeActivity, ReviewSpeakingActivity::class.java).apply {
                    putExtra(ReviewSpeakingActivity.EXTRA_NOTE_ID, noteId)
                    putExtra(ReviewSpeakingActivity.EXTRA_NOTE_CONTENT, noteContent)
                    putExtra(ReviewSpeakingActivity.KEY_HTP_CONFIG, htpConfigPath)
                    putExtra(ReviewSpeakingActivity.KEY_MODEL_NAME, modelName)
                }

                startActivityForResult(intent, REQUEST_REVIEW_SPEAKING)
            }
        }


        // 퀴즈 버튼
        binding.cardQuiz.setOnClickListener {
            Log.d("DEBUG", "cardQuiz 클릭됨")
            if (isComplete) {
                Toast.makeText(this, "이미 복습이 완료된 노트입니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

//            val memberId = getSharedPreferences("auth", MODE_PRIVATE).getLong("MEMBER_ID", -1)
//            if (memberId == -1L || noteId == null) return@setOnClickListener
            val memberId = 1L
            if (noteId == null) return@setOnClickListener

            Log.d("DEBUG", "퀴즈 리스트 요청 시작 (memberId=$memberId, noteId=$noteId)")
            lifecycleScope.launch {
                val quizList = ReviewRepository().getQuizzes(memberId, noteId!!)
                Log.d("DEBUG", "받은 퀴즈 개수: ${quizList.size}")
                if (quizList.isNotEmpty()) {
                    val intent = Intent(this@ReviewTypeActivity, QuizActivity::class.java).apply {
                        putExtra("ARTICLE_TITLE", articleTitle)
                        putExtra("ARTICLE_STAGE", articleStage)
                        putExtra("NOTE_ID", noteId)
                        putParcelableArrayListExtra("QUIZ_LIST", ArrayList(quizList))
                    }
                    startActivity(intent)
                } else {
                    Log.d("DEBUG", "퀴즈가 존재하지 않음")
                    Toast.makeText(this@ReviewTypeActivity, "퀴즈가 존재하지 않습니다.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    }


}