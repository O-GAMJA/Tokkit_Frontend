package com.example.tokkit

import android.graphics.Color
import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityBubbleChartBinding
import androidx.lifecycle.lifecycleScope
import com.example.tokkit.util.RetrofitClient
import com.google.gson.Gson
import android.util.Log
import kotlinx.coroutines.launch
import android.webkit.WebViewClient

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

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // 여기에서만 호출해야 JS 함수가 존재함
                fetchTagsAndInjectToWebView(1L)
            }
        }

        // 로컬 html 로드
        binding.webView.loadUrl("file:///android_asset/bubble_chart.html")
    }

    private fun fetchTagsAndInjectToWebView(memberId: Long) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.noteApi.getTagsByMemberId(memberId)
                if (response.isSuccess) {
                    val json = Gson().toJson(response.result)
                    val jsCode = "updateChart($json);"
                    binding.webView.evaluateJavascript(jsCode, null)
                }
            } catch (e: Exception) {
                Log.e("BubbleChart", "태그 데이터 불러오기 실패", e)
            }
        }
    }
}
