package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityRegisterNicknameBinding

class RegisterNicknameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterNicknameBinding
    private var userId: String = ""
    private var userPassword: String = ""
    private var isNicknameValid = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterNicknameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 이전 화면에서 전달받은 사용자 정보
        userId = intent.getStringExtra("USER_ID") ?: ""
        userPassword = intent.getStringExtra("USER_PASSWORD") ?: ""

        setupListeners()
        updateUI(false) // 초기 상태는 버튼 비활성화
    }

    private fun setupListeners() {
        // 닉네임 입력 리스너
        binding.etNickname.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val nickname = s.toString().trim()
                isNicknameValid = nickname.isNotEmpty()
                updateUI(isNicknameValid)
            }
        })

        // 다음 버튼 클릭 리스너
        binding.btnNext.setOnClickListener {
            if (isNicknameValid) {
                completeRegistration()
            }
        }
    }

    private fun updateUI(isValid: Boolean) {
        if (isValid) {
            binding.btnNext.visibility = View.VISIBLE
            binding.btnNextDisabled.visibility = View.GONE
        } else {
            binding.btnNext.visibility = View.GONE
            binding.btnNextDisabled.visibility = View.VISIBLE
        }
    }

    private fun completeRegistration() {
        val nickname = binding.etNickname.text.toString().trim()

        // DB 없이 그냥 회원가입 완료 처리 (모든 값 출력)
        Toast.makeText(this, "회원가입 완료! ID: $userId, 닉네임: $nickname", Toast.LENGTH_SHORT).show()

        // 메인 화면으로 이동
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}