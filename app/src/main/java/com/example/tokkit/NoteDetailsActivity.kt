package com.example.tokkit

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityNoteDetailsBinding

class NoteDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNoteDetailsBinding

    private val REQUEST_GALLERY_IMAGE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 이미지 업로드 버튼 - 터치한 위치 기준으로 팝업 표시
        binding.imageUpload.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val x = event.rawX.toInt()
                val y = event.rawY.toInt()
                showImageChoicePopupAt(x, y)
            }
            true
        }
    }

    private fun showImageChoicePopupAt(x: Int, y: Int) {
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.popup_image_choice, null)

        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindow.setBackgroundDrawable(ColorDrawable())
        popupWindow.isOutsideTouchable = true

        // 각 버튼 클릭 처리
        popupView.findViewById<LinearLayout>(R.id.btn_upload).setOnClickListener {
            openGallery()
            popupWindow.dismiss()
        }

        popupView.findViewById<LinearLayout>(R.id.btn_generate).setOnClickListener {
            // startActivity(Intent(this, ImageGeneratingActivity::class.java))
            popupWindow.dismiss()
        }

        // 터치한 좌표를 기준으로 팝업 띄우기 (왼쪽 상단 정렬)
        popupWindow.showAtLocation(binding.root, 0, x, y)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_GALLERY_IMAGE)
    }
}
