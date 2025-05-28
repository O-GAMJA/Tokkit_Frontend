package com.example.tokkit

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tokkit.data.remote.api.NoteApiService
import com.example.tokkit.databinding.ActivityBubbleChartBinding
import com.example.tokkit.util.RetrofitClient
import com.google.gson.Gson
import kotlinx.coroutines.launch

class BubbleChartActivity : AppCompatActivity() {

    private lateinit var binding : ActivityBubbleChartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBubbleChartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWebView()
        loadTagData()

        binding.btnBack.setOnClickListener{
            finish()
        }
    }

    private fun setupWebView() {
        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            // 투명 배경 설정
            setBackgroundColor(Color.TRANSPARENT)
            setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)

            // JavaScript Interface 추가 - 태그 클릭 이벤트 처리
            addJavascriptInterface(WebAppInterface(), "Android")

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // 페이지 로드 완료 후 태그 데이터 로드
                    loadTagData()
                }
            }

            loadUrl("file:///android_asset/bubble_chart.html")
        }
    }

    private fun loadTagData() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.noteApi.getTagsByMemberId(1L)
                if (response.isSuccess) {
                    val tagData = response.result
                    val jsonData = Gson().toJson(tagData)

                    // JavaScript 함수 호출하여 차트 업데이트
                    binding.webView.evaluateJavascript(
                        "updateChart($jsonData)", null
                    )
                } else {
                    Log.e("BubbleChart", "태그 데이터 로드 실패: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("BubbleChart", "예외 발생", e)
            }
        }
    }

    // WebView에서 호출할 수 있는 JavaScript Interface
    inner class WebAppInterface {
        @JavascriptInterface
        fun onTagClicked(tagName: String) {
            Log.d("BubbleChart", "태그 클릭됨: $tagName")

            // MainActivity로 돌아가면서 선택된 태그 정보 전달
            val intent = Intent(this@BubbleChartActivity, MainActivity::class.java).apply {
                putExtra("selected_tag", tagName)
                putExtra("search_by_tag", true)
                // MainActivity를 새로 시작하지 않고 기존 인스턴스로 돌아가기
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }
}