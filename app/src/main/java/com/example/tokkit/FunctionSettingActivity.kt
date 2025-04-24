package com.example.tokkit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokkit.adapter.FaqAdapter
import com.example.tokkit.databinding.ActivityFunctionSettingBinding
import com.example.tokkit.model.FaqItem

class FunctionSettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFunctionSettingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFunctionSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 뒤로가기
        binding.btnBack.setOnClickListener {
            finish()
        }

        val faqList = listOf(
            FaqItem("네트워크 연결이 되지 않았을 때 어떤 기능까지 사용할 수 있나요?", "내 노트작성, 내가 북마크한 노트조회, 노트 생성 기능이 가능합니다. 나머지 기능은 로그인이 필요해요 :)"),
            FaqItem("어떤 기능까지 사용할 수 있나요?", "로그인 없이도 간단한 사용은 가능합니다.")
        )

        val adapter = FaqAdapter(faqList)
        binding.rvFaq.adapter = adapter
        binding.rvFaq.layoutManager = LinearLayoutManager(this)
    }
}
