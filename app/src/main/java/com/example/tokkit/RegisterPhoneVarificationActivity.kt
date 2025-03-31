package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityRegisterPhoneVarificationBinding

class RegisterPhoneVarificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterPhoneVarificationBinding
    private var phoneNumber: String = ""
    private var verificationCode: String = "" // 실제로는 서버에서 받아야 함
    private lateinit var countDownTimer: CountDownTimer
    private var isVerified = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterPhoneVarificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 전달받은 전화번호
        phoneNumber = intent.getStringExtra("PHONE_NUMBER") ?: ""

        // 테스트용 랜덤 인증번호 생성 (실제로는 서버에서 받아야 함)
        verificationCode = "1234" // 테스트 코드로 고정

        setupUI()

        // 초기에는 오류 메시지 숨김
        binding.tvErrorMessage.visibility = View.GONE
    }

    private fun setupUI() {
        // 전화번호 표시
        binding.tvPhoneInfo.text = "${phoneNumber}로 인증번호를 보냈어요"

        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 인증번호 입력 필드에 텍스트 변경 리스너 추가
        binding.etVerificationCode.setOnEditorActionListener { _, _, _ ->
            validateCode()
            true
        }

        // 다음 버튼
        binding.btnNext.setOnClickListener {
            validateCode()
        }

        // 테스트를 위해 인증번호 출력
        Toast.makeText(this, "인증번호: $verificationCode", Toast.LENGTH_LONG).show()
    }

    private fun validateCode() {
        val inputCode = binding.etVerificationCode.text.toString().trim()

        if (inputCode.isEmpty()) {
            binding.tvErrorMessage.text = "인증번호를 입력해주세요"
            binding.tvErrorMessage.visibility = View.VISIBLE
            return
        }

        if (inputCode == verificationCode) {
            // 인증 성공
            isVerified = true
            binding.tvErrorMessage.visibility = View.GONE

            // 다음 화면으로 이동
            proceedToNextStep()
        } else {
            // 인증 실패
            binding.tvErrorMessage.text = "인증 번호가 일치하지않습니다."
            binding.tvErrorMessage.visibility = View.VISIBLE
        }
    }

    private fun proceedToNextStep() {
        // 회원가입 화면으로 이동
       // val intent = Intent(this, RegisterActivity::class.java)
        intent.putExtra("PHONE_NUMBER", phoneNumber)
        intent.putExtra("IS_VERIFIED", true)
        startActivity(intent)
        finish()
    }
}