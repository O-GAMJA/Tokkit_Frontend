package com.example.tokkit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityTagManageBinding

class TagManageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTagManageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTagManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 뒤로가기
        binding.btnBack.setOnClickListener {
            finish()
        }

    }
}
