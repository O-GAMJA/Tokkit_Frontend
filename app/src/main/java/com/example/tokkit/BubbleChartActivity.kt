package com.example.tokkit

import android.graphics.Color
import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityBubbleChartBinding

class BubbleChartActivity : AppCompatActivity() {

    private lateinit var binding : ActivityBubbleChartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBubbleChartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener{
            finish()
        }

        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.allowFileAccess = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.setBackgroundColor(Color.TRANSPARENT)

        // 로컬 html 로드
        binding.webView.loadUrl("file:///android_asset/bubble_chart.html")
    }
}
