package com.example.tokkit

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tokkit.databinding.ActivityChatBinding
import android.system.Os
import android.util.Log
import com.example.tokkit.genie.GenieConversationActivity
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths
import java.util.concurrent.Executors

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
            // 기존 애니메이션 관련 코드는 주석 처리
            /*if (!isListening) {
                binding.btnMic.visibility = View.INVISIBLE
                binding.lottieMic.visibility = View.VISIBLE
                binding.lottieMic.playAnimation()
                isListening = true
            } else {
                binding.lottieMic.cancelAnimation()
                binding.lottieMic.visibility = View.INVISIBLE
                binding.btnMic.visibility = View.VISIBLE
                isListening = false
            }*/

            // 대신 Genie 채팅 시작
            startGenieChat()
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
            val intent = Intent(this, NoteMarkdownActivity::class.java)
            startActivity(intent)
        }
    }

    // ======== Genie 모델을 사용한 채팅 기능 시작 ========
    private fun startGenieChat() {
        try {
            // 모델 및 설정 파일 준비
            copyAssetsIfNeeded()

            // HTP 설정 파일 경로 결정
            val socModel = android.os.Build.SOC_MODEL
            val htpConfig = when (socModel) {
                "SM8750" -> "qualcomm-snapdragon-8-elite.json"
                "SM8650" -> "qualcomm-snapdragon-8-gen3.json"
                "QCS8550" -> "qualcomm-snapdragon-8-gen2.json"
                else -> {
                    Toast.makeText(this, "지원되지 않는 디바이스입니다", Toast.LENGTH_LONG).show()
                    return
                }
            }

            // Genie 대화 시작
            val externalDir = externalCacheDir?.absolutePath ?: ""  // null 처리 추가
            val htpConfigPath = Paths.get(externalDir, "htp_config", htpConfig).toString()

            val intent = Intent(this, GenieConversationActivity::class.java).apply {
                putExtra(GenieConversationActivity.KEY_HTP_CONFIG, htpConfigPath)
                putExtra(GenieConversationActivity.KEY_MODEL_NAME, "llama3_2_3b")
            }
            startActivity(intent)

        } catch (e: Exception) {
            Toast.makeText(this, "Genie 초기화 중 오류: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("ChatActivity", "Error initializing Genie: ${e}")
        }
    }

    // 애셋 복사 함수
    private fun copyAssetsIfNeeded() {
        val externalDir = externalCacheDir?.absolutePath ?: ""  // null 처리 추가

        try {
            // models 디렉토리에 모델 파일 복사
            copyAssetDirectory("models", externalDir)

            // htp_config 디렉토리에 설정 파일 복사
            copyAssetDirectory("htp_config", externalDir)
        } catch (e: Exception) {
            Log.e("ChatActivity", "Error copying assets: ${e}")
            throw e
        }
    }

    // 애셋 디렉토리 복사 함수
    private fun copyAssetDirectory(assetPath: String, outputPath: String) {
        val outputDir = File(Paths.get(outputPath, assetPath).toString())
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val assetFiles = assets.list(assetPath) ?: return  // 변수명 변경 (assets -> assetFiles)

        for (asset in assetFiles) {
            val childAssetPath = "$assetPath/$asset"
            val childOutputPath = Paths.get(outputPath, childAssetPath).toString()

            try {
                val subAssets = assets.list(childAssetPath)  // 명시적으로 메서드 호출, 변수명 변경
                if (subAssets != null && subAssets.isNotEmpty()) {  // null 체크 추가 및 isEmpty 체크 방식 변경
                    // 디렉토리인 경우 재귀적으로 복사
                    copyAssetDirectory(childAssetPath, outputPath)
                } else {
                    // 파일인 경우 직접 복사
                    if (!File(childOutputPath).exists()) {
                        copyAssetFile(childAssetPath, childOutputPath)
                    }
                }
            } catch (e: Exception) {
                // 아직 파일 목록을 가져올 수 없는 경우 파일로 간주
                if (!File(childOutputPath).exists()) {
                    copyAssetFile(childAssetPath, childOutputPath)
                }
            }
        }
    }

    // 애셋 파일 복사 함수
    private fun copyAssetFile(assetPath: String, outputPath: String) {
        try {
            val input = assets.open(assetPath)
            val output = FileOutputStream(File(outputPath))

            val buffer = ByteArray(1024 * 1024)
            var read: Int

            while (input.read(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
            }

            input.close()
            output.close()
        } catch (e: Exception) {
            Log.e("ChatActivity", "Error copying asset file: $assetPath to $outputPath - ${e.message}")
        }
    }
    // ======== Genie 모델을 사용한 채팅 기능 끝 ========
}