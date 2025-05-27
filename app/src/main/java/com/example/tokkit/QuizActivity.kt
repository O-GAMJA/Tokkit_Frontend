package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.tokkit.data.remote.model.QuizItem
import com.example.tokkit.databinding.ActivityQuizXmlBinding

class QuizActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuizXmlBinding
    private var articleTitle: String? = null
    private var articleStage: Int = 0
    private var noteId: String? = null
    private var currentQuestionIndex = 0
    private var userSelectedOption = -1 // 사용자가 선택한 옵션 인덱스,, -1 = 미선택
    private val userAnswers = mutableMapOf<Int, Int>() // 사용자 답변 저장
    private val handler = Handler(Looper.getMainLooper())
    private var isShowingFeedback = false // 피드백 표시 중인지 여부

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizXmlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 인텐트에서 데이터 받기
        articleTitle = intent.getStringExtra("ARTICLE_TITLE")
        articleStage = intent.getIntExtra("ARTICLE_STAGE", 0)
        noteId = intent.getStringExtra("NOTE_ID")

        setupUI()
        val quizItems = intent.getParcelableArrayListExtra<QuizItem>("QUIZ_LIST") ?: return
        questions = quizItems.map {
            Question(it.question, it.choices, it.answer - 1)
        }
        showQuestion(currentQuestionIndex)

        // 피드백 오버레이 초기 설정
        binding.overlayView.visibility = View.GONE
        binding.correctMark.visibility = View.GONE
        binding.incorrectMark.visibility = View.GONE
    }

    private fun setupUI() {
        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 다음 버튼
        binding.btnNext.setOnClickListener {
            // 답을 선택했는지 확인
            if (userSelectedOption == -1) {
                // 답을 선택하지 않으면 -> 토스트메세지 처리 구현예정
                return@setOnClickListener
            }

            if (isShowingFeedback) {
                // 이미 피드백을 표시 중이면 무시
                return@setOnClickListener
            }

            // 정답 확인 및 피드백 표시
            checkAnswerAndShowFeedback()
        }

        // 선택지 버튼 클릭 리스너 설정
        binding.option1.setOnClickListener {
            if (!isShowingFeedback) selectOption(0)
        }
        binding.option2.setOnClickListener {
            if (!isShowingFeedback) selectOption(1)
        }
        binding.option3.setOnClickListener {
            if (!isShowingFeedback) selectOption(2)
        }
        binding.option4.setOnClickListener {
            if (!isShowingFeedback) selectOption(3)
        }
    }

    private fun showQuestion(index: Int) {
        val question = questions[index]
        binding.questionNumber.text = "Q${index + 1}."
        binding.questionText.text = question.text

        // 선택지 설정
        binding.option1.text = "1. ${question.options[0]}"
        binding.option2.text = "2. ${question.options[1]}"
        binding.option3.text = "3. ${question.options[2]}"
        binding.option4.text = "4. ${question.options[3]}"

        // 선택된 옵션 초기화
        resetOptionSelection()
    }

    private fun selectOption(optionIndex: Int) {
        // 이전 선택 초기화
        resetOptionSelection()

        // 현재 선택 저장
        userSelectedOption = optionIndex
        userAnswers[currentQuestionIndex] = optionIndex

        // 선택된 옵션 강조
        when (optionIndex) {
            0 -> {
                binding.option1.setBackgroundResource(R.drawable.bg_option_selected)
                binding.option1.setTextColor(resources.getColor(R.color.white, null))
            }
            1 -> {
                binding.option2.setBackgroundResource(R.drawable.bg_option_selected)
                binding.option2.setTextColor(resources.getColor(R.color.white, null))
            }
            2 -> {
                binding.option3.setBackgroundResource(R.drawable.bg_option_selected)
                binding.option3.setTextColor(resources.getColor(R.color.white, null))
            }
            3 -> {
                binding.option4.setBackgroundResource(R.drawable.bg_option_selected)
                binding.option4.setTextColor(resources.getColor(R.color.white, null))
            }
        }
    }

    private fun resetOptionSelection() {
        // 배경 초기화
        binding.option1.setBackgroundResource(R.drawable.bg_option_normal)
        binding.option2.setBackgroundResource(R.drawable.bg_option_normal)
        binding.option3.setBackgroundResource(R.drawable.bg_option_normal)
        binding.option4.setBackgroundResource(R.drawable.bg_option_normal)

        // 텍스트 색상 초기화
        binding.option1.setTextColor(resources.getColor(R.color.gray500, null))
        binding.option2.setTextColor(resources.getColor(R.color.gray500, null))
        binding.option3.setTextColor(resources.getColor(R.color.gray500, null))
        binding.option4.setTextColor(resources.getColor(R.color.gray500, null))
    }

    private fun updateProgressBar() {
        // 진행 상태 업데이트
        val progress = ((currentQuestionIndex + 1) * 100) / questions.size
        binding.progressBar.progress = progress
    }

    private fun checkAnswerAndShowFeedback() {
        isShowingFeedback = true
        val question = questions[currentQuestionIndex]
        val isCorrect = userSelectedOption == question.correctOptionIndex

        // 오버레이 표시
        binding.overlayView.visibility = View.VISIBLE

        // 정답/오답 표시
        if (isCorrect) {
            binding.correctMark.visibility = View.VISIBLE
            binding.incorrectMark.visibility = View.GONE
        } else {
            binding.correctMark.visibility = View.GONE
            binding.incorrectMark.visibility = View.VISIBLE

            // 틀렸을 경우 정답과 오답 표시
            highlightCorrectAndWrongAnswers(question.correctOptionIndex, userSelectedOption)
        }

        // 2초 후 다음 문제로 이동 또는 결과 화면으로 이동
        handler.postDelayed({
            binding.overlayView.visibility = View.GONE
            binding.correctMark.visibility = View.GONE
            binding.incorrectMark.visibility = View.GONE

            // 다음 문제 또는 결과 화면으로 이동
            if (currentQuestionIndex < questions.size - 1) {
                currentQuestionIndex++
                showQuestion(currentQuestionIndex)
                userSelectedOption = -1 // 선택 초기화
                updateProgressBar()
            } else {
                // 퀴즈 종료 처리
                finishQuiz()
            }

            isShowingFeedback = false
        }, 2000) // 2초 지연
    }

    private fun highlightCorrectAndWrongAnswers(correctIndex: Int, selectedIndex: Int) {
        // 정답은 초록색으로 표시
        val correctOption = when (correctIndex) {
            0 -> binding.option1
            1 -> binding.option2
            2 -> binding.option3
            3 -> binding.option4
            else -> null
        }
        correctOption?.setBackgroundResource(R.drawable.bg_option_correct)
        correctOption?.setTextColor(ContextCompat.getColor(this, R.color.white))

        // 틀린 답은 빨간색으로 표시 (선택한 답만)
        if (selectedIndex != correctIndex) {
            val wrongOption = when (selectedIndex) {
                0 -> binding.option1
                1 -> binding.option2
                2 -> binding.option3
                3 -> binding.option4
                else -> null
            }
            wrongOption?.setBackgroundResource(R.drawable.bg_option_wrong)
            wrongOption?.setTextColor(ContextCompat.getColor(this, R.color.white))
        }
    }

    private fun finishQuiz() {
        // 정답 수 계산
        var correctAnswers = 0
        for (i in questions.indices) {
            val selectedOption = userAnswers.getOrElse(i) { -1 }
            if (selectedOption == questions[i].correctOptionIndex) {
                correctAnswers++
            }
        }

        // 점수 계산 (각 문제당 20점)
        val score = correctAnswers * 20

        // 결과 화면으로 이동
        val intent = Intent(this, QuizResultActivity::class.java)
        intent.putExtra("NOTE_ID", noteId)
        intent.putExtra("CORRECT_COUNT", correctAnswers)
        intent.putExtra("SCORE", score)
        intent.putExtra("TOTAL_QUESTIONS", questions.size)
        intent.putExtra("USER_NAME", "Chamin") // *로그인 정보에서 가져올 예정
        startActivity(intent)
        finish()
    }

    // 문제 데이터 클래스
    data class Question(
        val text: String,
        val options: List<String>,
        val correctOptionIndex: Int
    )

    // 문제 리스트
    private lateinit var questions: List<Question>

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}