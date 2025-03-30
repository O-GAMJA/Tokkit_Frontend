package com.example.tokkit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityAttachReferenceBinding

class AttachReferenceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAttachReferenceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttachReferenceBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}