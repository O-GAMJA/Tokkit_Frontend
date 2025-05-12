package com.example.tokkit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.system.Os
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokkit.databinding.ActivityChatBinding
import com.example.tokkit.genie.ChatMessage
import com.example.tokkit.genie.ConversationManager
import com.example.tokkit.genie.GenieConversationActivity
import com.example.tokkit.genie.GenieWrapper
import com.example.tokkit.genie.MessageRecyclerViewAdapter
import com.example.tokkit.genie.MessageSender
import com.example.tokkit.genie.StringCallback
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths
import java.util.ArrayList
import java.util.Locale
import java.util.concurrent.Executors

class ChatActivity : AppCompatActivity(), ConversationManager.ConversationChangeListener {
    private lateinit var binding: ActivityChatBinding
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private lateinit var genieWrapper: GenieWrapper

    private val messages = ArrayList<ChatMessage>(1000)
    private var isListening = false
    private var fullResponse = StringBuilder()
    private var responseTimeoutHandler = Handler(Looper.getMainLooper())
    private var responseTimeoutRunnable: Runnable? = null

    private lateinit var adapter: MessageRecyclerViewAdapter


    companion object {
        private const val WELCOME_MESSAGE = "안녕하세요! 무엇을 도와드릴까요?"
        private const val REQUEST_RECORD_AUDIO = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 저장된 대화 내용 로드
        ConversationManager.loadConversation(this)

        adapter = MessageRecyclerViewAdapter(this, messages)
        binding.chatRecyclerView.adapter = adapter
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)

        // TTS 초기화
        initializeTTS()

        // 음성 권한 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
        } else {
            initializeRecognizer()
        }

        // ConversationManager에 리스너 등록
        ConversationManager.addListener(this)

        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 채팅 모드 변경 - GenieConversationActivity로 전환
        binding.btnChatMode.setOnClickListener {
            startGenieConversation()
            finish()
        }

        // 마이크 버튼 클릭 - 음성 인식 시작
        binding.btnMic.setOnClickListener {
            startSTT()
        }

        // 노트 생성 버튼
        binding.btnCreateNote.setOnClickListener {
            val intent = Intent(this, NoteMarkdownActivity::class.java)
            startActivity(intent)
        }

        // 기존 대화 내용이 있는지 확인하고 없으면 환영 메시지 추가
        val existingMessages = ConversationManager.getAllMessages()
        if (existingMessages.isEmpty()) {
            val welcomeMessage = ChatMessage(WELCOME_MESSAGE, MessageSender.BOT)
            ConversationManager.addMessage(welcomeMessage)
        } else {
            // ConversationManager에서 기존 대화 내용 로드
            loadMessagesFromManager()
        }

        // Genie 초기화
        initializeGenie()
    }

    private fun initializeTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US // 필요한 경우 Locale.KOREAN 등으로 변경 가능
            } else {
                Log.e("ChatActivity", "TTS 초기화 실패: $status")
            }
        }
    }

    private fun initializeRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull()
                if (!spokenText.isNullOrBlank()) {
                    handleUserInput(spokenText)
                }

                // 원래 마이크 버튼 복귀
                binding.lottieMic.cancelAnimation()
                binding.lottieMic.visibility = View.INVISIBLE
                binding.btnMic.visibility = View.VISIBLE
                isListening = false
            }

            override fun onError(error: Int) {
                Toast.makeText(this@ChatActivity, "음성 인식 오류 발생: $error", Toast.LENGTH_SHORT).show()

                // 원래 마이크 버튼 복귀
                binding.lottieMic.cancelAnimation()
                binding.lottieMic.visibility = View.INVISIBLE
                binding.btnMic.visibility = View.VISIBLE
                isListening = false
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startSTT() {
        if (!isListening) {
            // 애니메이션 시작
            binding.btnMic.visibility = View.INVISIBLE
            binding.lottieMic.visibility = View.VISIBLE
            binding.lottieMic.playAnimation()
            isListening = true

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US") // 필요시 "ko-KR"로 변경
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }

            try {
                speechRecognizer.startListening(intent)
            } catch (e: Exception) {
                Log.e("ChatActivity", "STT 시작 오류: ${e.message}")
                Toast.makeText(this, "음성 인식 시작 실패", Toast.LENGTH_SHORT).show()

                // 원래 마이크 버튼 복귀
                binding.lottieMic.cancelAnimation()
                binding.lottieMic.visibility = View.INVISIBLE
                binding.btnMic.visibility = View.VISIBLE
                isListening = false
            }
        } else {
            // 이미 듣고 있는 상태면 중단
            speechRecognizer.stopListening()
            binding.lottieMic.cancelAnimation()
            binding.lottieMic.visibility = View.INVISIBLE
            binding.btnMic.visibility = View.VISIBLE
            isListening = false
        }
    }

    private fun handleUserInput(message: String) {
        // ConversationManager를 통해 사용자 메시지 추가
        val userMessage = ChatMessage(message, MessageSender.USER)
        ConversationManager.addMessage(userMessage)

        // 응답 생성 요청
        if (::genieWrapper.isInitialized) {
            val executor = Executors.newSingleThreadExecutor()
            executor.execute {
                genieWrapper.getResponseForPrompt(message, object : StringCallback {
                    override fun onNewString(response: String) {
                        runOnUiThread {
                            fullResponse.append(response)

                            // ConversationManager를 통해 봇 메시지 업데이트
                            val updatedResponse = ConversationManager.updateLastBotMessage(response)

                            // 이전 대기 제거
                            responseTimeoutRunnable?.let { responseTimeoutHandler.removeCallbacks(it) }

                            // 새 대기 설정 - 발화를 위한 문장 처리
                            responseTimeoutRunnable = Runnable {
                                val finalText = fullResponse.toString()
                                val sentences = finalText
                                    .replace(",", "")
                                    .split(Regex("(?<=[.!?])\\s+"))

                                for (sentence in sentences) {
                                    val clean = sentence.replace(Regex("[.?!]$"), "").trim()
                                    if (clean.isNotBlank()) {
                                        tts.speak(clean, TextToSpeech.QUEUE_ADD, null, null)
                                    }
                                }

                                fullResponse.clear()
                            }
                            responseTimeoutHandler.postDelayed(responseTimeoutRunnable!!, 500)
                        }
                    }
                })
            }
        } else {
            Log.e("ChatActivity", "GenieWrapper가 초기화되지 않았습니다")
            Toast.makeText(this, "응답 생성기를 초기화하지 못했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeGenie() {
        try {
            // QNN 라이브러리 경로 설정
            val nativeLibPath = applicationContext.applicationInfo.nativeLibraryDir
            Os.setenv("ADSP_LIBRARY_PATH", nativeLibPath, true)
            Os.setenv("LD_LIBRARY_PATH", nativeLibPath, true)

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

            val externalDir = externalCacheDir?.absolutePath ?: ""
            val htpConfigPath = Paths.get(externalDir, "htp_config", htpConfig).toString()
            val modelDir = Paths.get(externalDir, "models", "llama3_2_3b").toString()

            genieWrapper = GenieWrapper(modelDir, htpConfigPath)
            Log.i("ChatActivity", "Genie 모델 로드 완료")

            // 기존 대화 내용이 있는지 확인하고 없으면 환영 메시지 추가
            val existingMessages = ConversationManager.getAllMessages()
            if (existingMessages.isEmpty()) {
                val welcomeMessage = ChatMessage(WELCOME_MESSAGE, MessageSender.BOT)
                ConversationManager.addMessage(welcomeMessage)
            } else {
                // ConversationManager에서 기존 대화 내용 로드
                loadMessagesFromManager()
            }


        } catch (e: Exception) {
            Log.e("ChatActivity", "Genie 초기화 오류: ${e.message}", e)
            Toast.makeText(this, "Genie 초기화 중 오류 발생: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadMessagesFromManager() {
        messages.clear()
        messages.addAll(ConversationManager.getAllMessages())
        adapter.notifyDataSetChanged()
        if (messages.isNotEmpty()) {
            binding.chatRecyclerView.scrollToPosition(messages.size - 1)
        }
        Log.d("ChatActivity", "loadMessagesFromManager(): 메시지 ${messages.size}개 로딩됨")
        for ((index, message) in messages.withIndex()) {
            Log.d("ChatActivity", "[$index] ${if (message.isMessageFromUser()) "USER" else "BOT"}: ${message.getMessage()}")
        }
    }

    private fun startGenieConversation() {
        try {
            val externalDir = externalCacheDir?.absolutePath ?: ""

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

            val htpConfigPath = Paths.get(externalDir, "htp_config", htpConfig).toString()

            val intent = Intent(this, GenieConversationActivity::class.java).apply {
                putExtra(GenieConversationActivity.KEY_HTP_CONFIG, htpConfigPath)
                putExtra(GenieConversationActivity.KEY_MODEL_NAME, "llama3_2_3b")
            }
            startActivity(intent)


        } catch (e: Exception) {
            Log.e("ChatActivity", "Genie 대화 시작 오류: ${e.message}", e)
            Toast.makeText(this, "Genie 대화 시작 중 오류 발생: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 애셋 복사 함수
    private fun copyAssetsIfNeeded() {
        val externalDir = externalCacheDir?.absolutePath ?: ""

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

        val assetFiles = assets.list(assetPath) ?: return

        for (asset in assetFiles) {
            val childAssetPath = "$assetPath/$asset"
            val childOutputPath = Paths.get(outputPath, childAssetPath).toString()

            try {
                val subAssets = assets.list(childAssetPath)
                if (subAssets != null && subAssets.isNotEmpty()) {
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

    // ConversationManager.ConversationChangeListener 구현
    override fun onConversationChanged(updatedMessages: List<ChatMessage>) {
        messages.clear()
        messages.addAll(updatedMessages)
        // 필요시 로그나 TTS 발화도 가능
        Log.d("ChatActivity", "onConversationChanged(): 메시지 ${messages.size}개 로딩됨")
        for ((index, message) in messages.withIndex()) {
            Log.d("ChatActivity", "[$index] ${if (message.isMessageFromUser()) "USER" else "BOT"}: ${message.getMessage()}")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeRecognizer()
        } else {
            Toast.makeText(this, "음성 인식을 위해 마이크 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        // 활동이 중지될 때 대화 상태 저장
        ConversationManager.saveConversation(this)
    }

    override fun onDestroy() {
        // ConversationManager 리스너 제거
        ConversationManager.removeListener(this)

        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }

        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }

        super.onDestroy()
    }
}