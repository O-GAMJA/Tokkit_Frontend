package com.example.tokkit

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityAttachReferenceBinding

class AttachReferenceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAttachReferenceBinding

    private val PICK_IMAGE_REQUEST = 1001
    private val PICK_FILE_REQUEST = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAttachReferenceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //  뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 사진 첨부 버튼
        binding.btnAttachPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // 파일 첨부 버튼
        binding.btnAttachFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            startActivityForResult(intent, PICK_FILE_REQUEST)
        }

        //  건너뛰기 버튼
        binding.btnSkip.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }
    }

    // 첨부 결과 처리
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {
            val uri: Uri? = data.data
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    // TODO: 이미지 URI 사용 예시
                    uri?.let {
                        // 예: Glide.with(this).load(it).into(binding.imageViewPreview)
                    }
                }
                PICK_FILE_REQUEST -> {
                    // TODO: 파일 URI 처리
                    uri?.let {
                        // 예: Log.d("FileURI", it.toString())
                    }
                }
            }
        }
    }
}
