package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        // Google 로그인 버튼
        binding.btnGoogleLogin.setOnClickListener {
            // Google 로그인 구현

            // 일단은 메인 화면으로 이동
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // 다른 방법으로 로그인 버튼
        binding.btnOtherLogin.setOnClickListener {
            // 이메일/비밀번호 로그인 화면으로 이동
            //startActivity(Intent(this, LoginEmailActivity::class.java))
        }

        // 회원가입 텍스트
        binding.tvSignUp.setOnClickListener {
            // 회원가입 화면으로 이동
            // startActivity(Intent(this, SignUpActivity::class.java))
        }
    }
}