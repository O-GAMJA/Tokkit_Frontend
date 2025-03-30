package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY = 2000L // 2초 동안 스플래시 화면 표시

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 상태바(Status Bar) 숨기기 -> 전체 화면 효과
        supportActionBar?.hide()

        // 일정 시간 후 메인 액티비티로 이동
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish() // 스플래시 액티비티 종료
        }, SPLASH_DELAY)
    }
}