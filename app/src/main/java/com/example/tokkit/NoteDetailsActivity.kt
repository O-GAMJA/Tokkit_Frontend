package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityNoteDetailsBinding

class NoteDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNoteDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //  뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 이미지 업로드 버튼
        binding.imageUpload.setOnClickListener {
            val options = arrayOf("사진 업로드", "AI로 사진 생성")
            AlertDialog.Builder(this)
                .setTitle("이미지 선택")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            // 사진 업로드 처리
                            openGallery()
                        }
                        1 -> {
                            // AI 이미지 생성 로딩 화면으로 이동
                            //startActivity(Intent(this, ImageGeneratingActivity::class.java))
                        }
                    }
                }
                .show()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        //startActivityForResult(intent, REQUEST_GALLERY_IMAGE)
    }
}