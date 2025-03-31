package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityChatBinding


class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding

    private var isListening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //  뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 채팅 모드 변경
        binding.btnChatMode.setOnClickListener {
            startActivity(Intent(this, ChatTextActivity::class.java))
            finish()
        }

        // 🎙️ 마이크 클릭 - 음성 인식 시작 (임시로 토스트 처리)
        binding.btnMic.setOnClickListener {
            if (!isListening) {
//                Toast.makeText(this, "음성 인식을 시작합니다", Toast.LENGTH_SHORT).show()
                // TODO: 이후 SpeechRecognizer 연동 가능

                // 🔊 애니메이션 시작
                binding.btnMic.visibility = View.INVISIBLE
                binding.lottieMic.visibility = View.VISIBLE
                binding.lottieMic.playAnimation()
                isListening = true
            } else {
                // 🔇 원래 마이크 버튼 복귀
                binding.lottieMic.cancelAnimation()
                binding.lottieMic.visibility = View.INVISIBLE
                binding.btnMic.visibility = View.VISIBLE
                isListening = false
            }
        }

        // 🎙️ 마이크 클릭 - 음성 인식 시작 (임시로 토스트 처리)
        binding.lottieMic.setOnClickListener {
            if (!isListening) {
//                Toast.makeText(this, "음성 인식을 시작합니다", Toast.LENGTH_SHORT).show()
                // TODO: 이후 SpeechRecognizer 연동 가능

                // 🔊 애니메이션 시작
                binding.btnMic.visibility = View.INVISIBLE
                binding.lottieMic.visibility = View.VISIBLE
                binding.lottieMic.playAnimation()
                isListening = true
            } else {
                // 🔇 원래 마이크 버튼 복귀
                binding.lottieMic.cancelAnimation()
                binding.lottieMic.visibility = View.INVISIBLE
                binding.btnMic.visibility = View.VISIBLE
                isListening = false
            }
        }


        // 📒 노트 생성 버튼
        binding.btnCreateNote.setOnClickListener {
            // TODO: 생성된 노트를 저장하고 다음 화면으로 넘김
//            val intent = Intent(this, NoteResultActivity::class.java)
//            startActivity(intent)
        }
    }
}