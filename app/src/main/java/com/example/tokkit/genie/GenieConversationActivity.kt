package com.example.tokkit.genie

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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokkit.databinding.ActivityGenieChatBinding
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Executors

class GenieConversationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGenieChatBinding
    private val messages = ArrayList<ChatMessage>(1000)
    private lateinit var adapter: MessageRecyclerViewAdapter
    private lateinit var genieWrapper: GenieWrapper
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private var fullResponse = StringBuilder()
    // 1초 동안 새 토큰이 없으면 응답 종료로 간주
    private var responseTimeoutHandler = Handler(Looper.getMainLooper())
    private var responseTimeoutRunnable: Runnable? = null

    private val responseQueue: Queue<String> = LinkedList()
    private var displayText = StringBuilder()
    private var isSpeaking = false


    companion object {
        private const val WELCOME_MESSAGE = "안녕하세요! 무엇을 도와드릴까요?"
        const val KEY_HTP_CONFIG = "htp_config_path"
        const val KEY_MODEL_NAME = "model_dir_name"
        private const val REQUEST_RECORD_AUDIO = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGenieChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = MessageRecyclerViewAdapter(this, messages)
        binding.chatRecyclerView.adapter = adapter
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)

        binding.btnBack.setOnClickListener { finish() }

        // TTS 초기화
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US // 필요한 경우 Locale.US 등으로 변경
            }
        }

        // 음성 권한 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
        } else {
            initializeRecognizer()
        }

        try {
            val nativeLibPath = applicationContext.applicationInfo.nativeLibraryDir
            Os.setenv("ADSP_LIBRARY_PATH", nativeLibPath, true)
            Os.setenv("LD_LIBRARY_PATH", nativeLibPath, true)

            val bundle = intent.extras
            if (bundle == null) {
                Log.e("GenieChat", "설정 정보 누락")
                Toast.makeText(this, "설정 정보를 가져오지 못했습니다.", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            val htpConfigPath = bundle.getString(KEY_HTP_CONFIG)
            val modelName = bundle.getString(KEY_MODEL_NAME)
            val externalCacheDir = this.externalCacheDir?.absolutePath
            val modelDir = Paths.get(externalCacheDir, "models", modelName).toString()

            genieWrapper = GenieWrapper(modelDir, htpConfigPath)
            Log.i("GenieChat", "$modelName 모델 로드 완료")

            messages.add(ChatMessage(WELCOME_MESSAGE, MessageSender.BOT))

            binding.btnMic.setOnClickListener {
                startSTT()
            }

        } catch (e: Exception) {
            Log.e("GenieChat", "에러: ${e}")
            Toast.makeText(this, "초기화 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull()
                if (!spokenText.isNullOrBlank()) {
                    sendMessage(spokenText)
                }
            }

            override fun onError(error: Int) {
                Toast.makeText(this@GenieConversationActivity, "STT 오류 발생: $error", Toast.LENGTH_SHORT).show()
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
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")  // 또는 "en-US"
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        speechRecognizer.startListening(intent)
    }

    private fun sendMessage(message: String) {
        adapter.addMessage(ChatMessage(message, MessageSender.USER))
        adapter.notifyItemInserted(adapter.itemCount - 1)
        binding.chatRecyclerView.smoothScrollToPosition(adapter.itemCount)

        val index = adapter.itemCount
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            genieWrapper.getResponseForPrompt(message, object : StringCallback {
                override fun onNewString(response: String) {
                    runOnUiThread {
                        fullResponse.append(response)
                        Log.d("GenieResponse", "AI 전체 응답: ${fullResponse.toString().replace("\n", "\\n")}")

                        adapter.updateBotMessage(response)
                        adapter.notifyItemChanged(index)

                        // 이전 대기 제거
                        responseTimeoutRunnable?.let { responseTimeoutHandler.removeCallbacks(it) }

                        // 새 대기 설정
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
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeRecognizer()
        } else {
            Toast.makeText(this, "음성 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
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
