package com.example.tokkit

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityGeneratedResultBinding
import java.io.ByteArrayOutputStream

class GeneratedResultActivity : AppCompatActivity() {

    private lateinit var binding : ActivityGeneratedResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGeneratedResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.saveText.setOnClickListener {
            // ImageView에서 Bitmap 추출
            val drawable = binding.generatedImage.drawable
            val bitmap = (drawable as BitmapDrawable).bitmap

            // Bitmap을 byte array로 변환
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()

            // Intent로 전달
            val intent = Intent(this, NoteDetailsActivity::class.java)
            intent.putExtra("generatedImage", byteArray)
            startActivity(intent)
            finish()
        }


        binding.regenerateButton.setOnClickListener{
            val intent = Intent(this, LoadingActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}