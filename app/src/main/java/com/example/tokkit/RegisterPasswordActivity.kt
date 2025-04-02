package com.example.tokkit

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityRegisterPasswordBinding

class RegisterPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterPasswordBinding
    private var userId: String = ""
    private var isPasswordValid = false

    // 비밀번호 유효성 검사용 정규식
    private val hasLengthPattern = ".{8,}".toRegex()
    private val hasNumberPattern = ".*[0-9].*".toRegex()
    private val hasAlphabetPattern = ".*[a-zA-Z].*".toRegex()
    private val hasSpecialCharPattern = ".*[!@#$%^&*()\\-_=+\\\\|\\[{\\]};:'\",<.>/?].*".toRegex()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 이전 화면에서 전달받은 사용자 ID
        userId = intent.getStringExtra("USER_ID") ?: ""

        setupListeners()
        updateUI("") // 초기 상태
    }

    private fun setupListeners() {
        // 비밀번호 입력 리스너
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                validatePassword(password)
            }
        })

        // 다음 버튼 클릭 리스너
        binding.btnNext.setOnClickListener {
            if (isPasswordValid) {
                val password = binding.etPassword.text.toString()
                val intent = Intent(this, RegisterNicknameActivity::class.java)
                intent.putExtra("USER_ID", userId)
                intent.putExtra("USER_PASSWORD", password)
                startActivity(intent)
            }
        }
    }

    private fun validatePassword(password: String) {
        // 각 요구사항 검사
        val hasLength = password.matches(hasLengthPattern)
        val hasNumber = password.matches(hasNumberPattern)
        val hasAlphabet = password.matches(hasAlphabetPattern)
        val hasSpecialChar = password.matches(hasSpecialCharPattern)

        // 모든 요구사항이 충족되었는지 확인
        isPasswordValid = hasLength && hasNumber && hasAlphabet && hasSpecialChar

        // UI 업데이트
        updateUI(password, hasLength, hasNumber, hasAlphabet, hasSpecialChar)
    }

    private fun updateUI(
        password: String,
        hasLength: Boolean = false,
        hasNumber: Boolean = false,
        hasAlphabet: Boolean = false,
        hasSpecialChar: Boolean = false
    ) {
        // 요구사항별 UI 업데이트
        binding.tvReqLength.setTextColor(if (hasLength) Color.parseColor("#C89AFF") else Color.GRAY)
        binding.tvReqNumber.setTextColor(if (hasNumber) Color.parseColor("#C89AFF") else Color.GRAY)
        binding.tvReqAlphabet.setTextColor(if (hasAlphabet) Color.parseColor("#C89AFF") else Color.GRAY)
        binding.tvReqSpecialChar.setTextColor(if (hasSpecialChar) Color.parseColor("#C89AFF") else Color.GRAY)

        if (password.isEmpty()) {
            // 빈 입력 상태
            binding.tvStatusError.visibility = View.GONE
            binding.btnNext.visibility = View.GONE
            binding.btnNextDisabled.visibility = View.VISIBLE
        } else if (isPasswordValid) {
            // 유효한 비밀번호
            binding.tvStatusError.visibility = View.GONE
            binding.btnNext.visibility = View.VISIBLE
            binding.btnNextDisabled.visibility = View.GONE
        } else {
            // 유효하지 않은 비밀번호
            binding.tvStatusError.visibility = View.VISIBLE
            binding.btnNext.visibility = View.GONE
            binding.btnNextDisabled.visibility = View.VISIBLE
        }
    }
}