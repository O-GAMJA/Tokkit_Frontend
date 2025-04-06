package com.example.tokkit

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.tokkit.databinding.ActivityNoteDetailsBinding

class NoteDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNoteDetailsBinding

    private var currentTagList = mutableListOf<String>()
    private var selectedPath: String? = null
    private val REQUEST_GALLERY_IMAGE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // 태그 초기화
        val tagList = intent.getStringArrayListExtra("selectedTags")
        if (!tagList.isNullOrEmpty()) {
            binding.tagContent.visibility = View.INVISIBLE
            binding.tagContainerInNote.visibility = View.VISIBLE
            renderSelectedTags(tagList)
        }

        // 경로 초기화
        selectedPath = intent.getStringExtra("selectedPath")
        if (!selectedPath.isNullOrEmpty()) {
            binding.storageDetail.text = selectedPath
        }

        val byteArray = intent.getByteArrayExtra("generatedImage")
        if (byteArray != null) {
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            binding.imageUpload.setImageBitmap(bitmap)

            // 크기 조정
            val layoutParams = binding.imageUpload.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.width = dpToPx(300)
            layoutParams.height = dpToPx(300)

            // marginTop 24dp로 변경
            layoutParams.topMargin = dpToPx(0)

            binding.imageUpload.layoutParams = layoutParams

            // 스케일 설정
            binding.imageUpload.scaleType = ImageView.ScaleType.CENTER_CROP
            binding.imageUpload.adjustViewBounds = true
            binding.imageUpload.requestLayout()
        }


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

        binding.tagForward.setOnClickListener {
            val intent = Intent(this, TagManageActivity::class.java)
            intent.putStringArrayListExtra("existingTags", ArrayList(currentTagList))
            startActivityForResult(intent, 101)
        }

        //저장 위치 화살표 버튼
        binding.storageForward.setOnClickListener{
            val intent = Intent(this, SaveLocationActivity::class.java)
            startActivityForResult(intent, 102)
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
            // TODO: 이미지 생성 로직 구현 ( Stable Diffusion )
            val intent = Intent(this, LoadingActivity::class.java)
            intent.putStringArrayListExtra("selectedTags", ArrayList(currentTagList))
            intent.putExtra("selectedPath", selectedPath)
            popupWindow.dismiss()
            startActivity(intent)
            finish()
        }

        // 터치한 좌표를 기준으로 팝업 띄우기 (왼쪽 상단 정렬)
        popupWindow.showAtLocation(binding.root, 0, x, y)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_GALLERY_IMAGE)
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK) {
            val tagList = data?.getStringArrayListExtra("selectedTags") ?: return
            if (tagList.isNotEmpty()) {
                binding.tagContent.visibility = View.INVISIBLE      // 안내 문구 숨김 (공간 유지)
                binding.tagContainerInNote.visibility = View.VISIBLE  // 칩 영역 표시
                renderSelectedTags(tagList)
            }
        }
        if (requestCode == 102 && resultCode == RESULT_OK) {
            val path = data?.getStringExtra("selectedPath")
            if (!path.isNullOrEmpty()) {
                binding.storageDetail.text = path
                selectedPath = path
            }
        }
    }

    private fun renderSelectedTags(tags: List<String>) {
        currentTagList = tags.toMutableList()
        val tagContainer = binding.tagContainerInNote
        tagContainer.removeAllViews()

        val inflater = LayoutInflater.from(this)
        for (tag in tags) {
            val chip = inflater.inflate(R.layout.item_chip, tagContainer, false) as TextView
            chip.text = "# $tag"
            tagContainer.addView(chip)
        }
    }
}