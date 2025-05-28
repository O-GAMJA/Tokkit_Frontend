package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityLoginEmailBinding
import android.util.Log
import com.example.tokkit.util.RetrofitClient
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.tokkit.data.remote.model.LoginRequest
import com.example.tokkit.util.CustomToastUtil



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
                CustomToastUtil.showToast(
                    context = this,
                    message = "이메일과 비밀번호를 입력해주세요",
                    iconResId = R.drawable.ic_bot
                )
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
                            CustomToastUtil.showToast(
                                context = this@LoginEmailActivity,
                                message = "로그인 실패: ${response.code()}",
                                iconResId = R.drawable.ic_bot
                            )
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        CustomToastUtil.showToast(
                            context = this@LoginEmailActivity,
                            message = "서버 오류: ${t.localizedMessage}",
                            iconResId = R.drawable.ic_bot
                        )
                        Log.e("Login", "로그인 요청 실패", t)
                    }
                })
            }
        }
    }
}

