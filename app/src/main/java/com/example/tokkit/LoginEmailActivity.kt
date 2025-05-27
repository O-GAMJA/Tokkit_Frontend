package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityLoginEmailBinding

/*
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
}*/

import android.util.Log
import com.example.tokkit.util.RetrofitClient
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.tokkit.data.remote.model.LoginRequest



class LoginEmailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginEmailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔥 FCM 토큰 가져와서 함께 로그인 요청
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCM", "FCM 토큰 가져오기 실패", task.exception)
                    return@addOnCompleteListener
                }

                val fcmToken = task.result
                val loginRequest = LoginRequest(email, password, fcmToken)

                // Retrofit으로 로그인 API 요청
                RetrofitClient.authApi.login(loginRequest).enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            Log.d("Login", "로그인 성공")
                            startActivity(Intent(this@LoginEmailActivity, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@LoginEmailActivity, "로그인 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Toast.makeText(this@LoginEmailActivity, "서버 오류: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                        Log.e("Login", "로그인 요청 실패", t)
                    }
                })
            }
        }
    }
}

