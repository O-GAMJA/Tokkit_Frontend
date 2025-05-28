package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityRegisterPhoneBinding
import com.example.tokkit.util.CustomToastUtil

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
            CustomToastUtil.showToast(
                context = this,
                message = "전화번호를 입력해주세요",
                iconResId = R.drawable.ic_bot
            )
            return
        }

        // 전화번호 형식 검사 (간단한 검사)
        if (!phoneNumber.matches(Regex("^01[0-9]{8,9}$"))) {
            CustomToastUtil.showToast(
                context = this,
                message = "올바른 전화번호 형식이 아닙니다",
                iconResId = R.drawable.ic_bot
            )
            return
        }

        // 인증번호 화면으로 이동
        val intent = Intent(this, RegisterPhoneVarificationActivity::class.java)
        intent.putExtra("PHONE_NUMBER", phoneNumber)
        startActivity(intent)
    }
}