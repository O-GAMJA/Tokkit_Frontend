package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityEditNicknameBinding

class EditNicknameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditNicknameBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditNicknameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnSave.setOnClickListener {
            val newNickname = binding.editNickname.text.toString()

            val resultIntent = Intent()
            resultIntent.putExtra("NEW_NICKNAME", newNickname)
            setResult(RESULT_OK, resultIntent)

            Toast.makeText(this, "닉네임이 저장되었습니다", Toast.LENGTH_SHORT).show()
            finish()
        }

    }
}