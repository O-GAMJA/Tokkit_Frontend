package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityRegisterPhoneBinding
class RegisterPhoneActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterPhoneBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterPhoneBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 다음 버튼
        binding.btnNext.setOnClickListener {
            validateAndProceed()
        }
    }

    private fun validateAndProceed() {
        val phoneNumber = binding.etPhoneNumber.text.toString().trim()

        // 전화번호 유효성 검사
        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "전화번호를 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        // 전화번호 형식 검사 (간단한 검사)
        if (!phoneNumber.matches(Regex("^01[0-9]{8,9}$"))) {
            Toast.makeText(this, "올바른 전화번호 형식이 아닙니다", Toast.LENGTH_SHORT).show()
            return
        }

        // 인증번호 화면으로 이동
//        val intent = Intent(this, VerificationCodeActivity::class.java)
        intent.putExtra("PHONE_NUMBER", phoneNumber)
        startActivity(intent)
    }
}