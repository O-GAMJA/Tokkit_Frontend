package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityLoadingBinding

class LoadingActivity : AppCompatActivity() {

    private lateinit var binding : ActivityLoadingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tagList = intent.getStringArrayListExtra("selectedTags") ?: arrayListOf()

        binding.imageGenerate.setOnClickListener {
            val intent = Intent(this, GeneratedResultActivity::class.java)
            intent.putStringArrayListExtra("selectedTags", tagList)
            startActivity(intent)
            finish()
        }
    }
}