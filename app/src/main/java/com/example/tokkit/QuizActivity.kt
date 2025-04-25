package com.example.tokkit

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityQuizXmlBinding

class QuizActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuizXmlBinding
    private var articleTitle: String? = null
    private var articleStage: Int = 0
    private var currentQuestionIndex = 0
    private var userSelectedOption = -1 // 사용자가 선택한 옵션 인덱스,, -1 = 미선택

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizXmlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 인텐트에서 데이터 받기
        articleTitle = intent.getStringExtra("ARTICLE_TITLE")
        articleStage = intent.getIntExtra("ARTICLE_STAGE", 0)

        setupUI()
        setupQuestions()
        showQuestion(currentQuestionIndex)
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

            // 다음 질문으로 이동
            if (currentQuestionIndex < questions.size - 1) {
                currentQuestionIndex++
                showQuestion(currentQuestionIndex)
                userSelectedOption = -1 // 선택 초기화
                updateProgressBar()
            } else {
                // 퀴즈 종료 처리
                finishQuiz()
            }
        }

        // 선택지 버튼 클릭 리스너 설정
        binding.option1.setOnClickListener { selectOption(0) }
        binding.option2.setOnClickListener { selectOption(1) }
        binding.option3.setOnClickListener { selectOption(2) }
        binding.option4.setOnClickListener { selectOption(3) }
    }

    private fun setupQuestions() {
        // 예시 문제 데이터
        questions = listOf(
            Question(
                "우리나라 최초의 한글 소설은?",
                listOf("홍길동전", "춘향전", "별주부전", "심청전"),
                0
            ),
            Question(
                "TCP/IP에서 IP는 무엇의 약자인가?",
                listOf("Internet Protocol", "Internal Protocol", "Interface Program", "Information Process"),
                0
            ),
            Question(
                "OSI 7계층에서 물리 계층은 몇 번째 계층인가?",
                listOf("1계층", "2계층", "3계층", "7계층"),
                0
            ),
            Question(
                "라우터가 동작하는 OSI 계층은?",
                listOf("물리 계층", "데이터 링크 계층", "네트워크 계층", "응용 계층"),
                2
            ),
            Question(
                "IPv4 주소의 비트 수는?",
                listOf("8비트", "16비트", "32비트", "64비트"),
                2
            )
        )

        // 프로그레스바 초기 설정
        updateProgressBar()
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

    private fun finishQuiz() {
        //결과 액티비티
        // val intent = Intent(this, QuizResultActivity::class.java)
        // startActivity(intent)
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
}