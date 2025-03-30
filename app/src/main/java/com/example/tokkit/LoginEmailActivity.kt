package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityLoginEmailBinding

class LoginEmailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginEmailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 로그인 버튼
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // 입력 검증 일단 토스트메세지
//            if (email.isEmpty()) {
//                Toast.makeText(this, "이메일을 입력해주세요", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            if (password.isEmpty()) {
//                Toast.makeText(this, "비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }

            // 로그인 처리 일단 메인 화면으로
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}