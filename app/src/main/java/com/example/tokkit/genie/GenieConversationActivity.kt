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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokkit.R
import com.example.tokkit.databinding.ActivityGenieChatBinding
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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

    private val markdownPromptHandler = MarkdownPromptHandler()
    private var isListening = false

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

        // 뒤로가기 버튼 이벤트
        binding.btnBack.setOnClickListener {
            finish()
        }

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
            // QNN 라이브러리 경로 설정
            val nativeLibPath = applicationContext.applicationInfo.nativeLibraryDir
            Os.setenv("ADSP_LIBRARY_PATH", nativeLibPath, true)
            Os.setenv("LD_LIBRARY_PATH", nativeLibPath, true)

            // Intent에서 설정 정보 가져오기
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

            // 마이크 버튼 클릭 시 STT 시작
            binding.btnMic.setOnClickListener {
                startSTT()
            }

            // 채팅 모드 전환 버튼
            binding.btnChatMode.setOnClickListener {
                // 여기서는 채팅 텍스트 모드로 전환하는
                // 현재는 ChatTextActivity로 전환하는 코드있어 생략
            }

            // 노트 생성 버튼
            binding.btnCreateNote.setOnClickListener {
                createMarkdownNote()
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

                // 원래 마이크 버튼 복귀
                binding.lottieMic.cancelAnimation()
                binding.lottieMic.visibility = View.INVISIBLE
                binding.btnMic.visibility = View.VISIBLE
                isListening = false
            }

            override fun onError(error: Int) {
                Toast.makeText(this@GenieConversationActivity, "STT 오류 발생: $error", Toast.LENGTH_SHORT).show()

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
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }

            speechRecognizer.startListening(intent)
        } else {
            // 이미 듣고 있는 상태면 중단
            speechRecognizer.stopListening()
            binding.lottieMic.cancelAnimation()
            binding.lottieMic.visibility = View.INVISIBLE
            binding.btnMic.visibility = View.VISIBLE
            isListening = false
        }
    }

    private fun sendMessage(message: String) {
        // 사용자 메시지 추가
        adapter.addMessage(ChatMessage(message, MessageSender.USER))
        adapter.notifyItemInserted(adapter.itemCount - 1)
        binding.chatRecyclerView.smoothScrollToPosition(adapter.itemCount)

        val index = adapter.itemCount

        // BOT 메시지 위치를 위한 빈 메시지 추가
        adapter.addMessage(ChatMessage("", MessageSender.BOT))
        adapter.notifyItemInserted(adapter.itemCount - 1)

        val executor = Executors.newSingleThreadExecutor()

        // 문장 버퍼 추가
        val sentenceBuffer = StringBuilder()
        fullResponse.clear() // 이전 응답 초기화

        executor.execute {
            genieWrapper.getResponseForPrompt(message, object : StringCallback {
                override fun onNewString(response: String) {
                    runOnUiThread {
                        // 누적 응답에 새 응답 추가
                        fullResponse.append(response)
                        sentenceBuffer.append(response)

                        // UI 업데이트
                        messages[adapter.itemCount - 1] = ChatMessage(fullResponse.toString(), MessageSender.BOT)
                        adapter.notifyItemChanged(adapter.itemCount - 1)
                        binding.chatRecyclerView.scrollToPosition(adapter.itemCount - 1)

                        // 문장 단위로 TTS 실행
                        val bufferStr = sentenceBuffer.toString()
                        val sentenceEndPattern = Regex("[.,?!]")

                        if (sentenceEndPattern.containsMatchIn(bufferStr)) {
                            // 문장 끝 부호를 기준으로 분리
                            val sentences = bufferStr.split(sentenceEndPattern)

                            // 마지막 문장(아직 완성되지 않은)을 제외하고 처리
                            if (sentences.size > 1) {
                                for (i in 0 until sentences.size - 1) {
                                    val sentence = sentences[i].trim()
                                    if (sentence.isNotBlank()) {
                                        tts.speak(sentence, TextToSpeech.QUEUE_ADD, null, null)
                                    }
                                }

                                // 버퍼 초기화 후 마지막 미완성 문장만 유지
                                sentenceBuffer.clear()
                                sentenceBuffer.append(sentences.last())
                            }
                        }
                    }
                }
            })
        }
    }

    // 마크다운 노트 생성 메서드
    private fun createMarkdownNote() {
        // 대화 내용 추출
        val conversationBuilder = StringBuilder()
        for (message in messages) {
            val sender = if (message.isMessageFromUser()) "User" else "Assistant"
            conversationBuilder.append("$sender: ${message.getMessage()}\n\n")
        }

        val conversation = conversationBuilder.toString()

        // 로그 추가
        Log.d("GenieChat", "대화 내용: $conversation")

        // 마크다운 노트 생성을 위한 프롬프트
        val notePrompt = markdownPromptHandler.getPromptForNoteGeneration(conversation)

        Toast.makeText(this, "노트 생성 중...", Toast.LENGTH_SHORT).show()

        // 백그라운드에서 노트 생성
        val service = Executors.newSingleThreadExecutor()
        service.execute {
            try {
                // 응답을 완전히 받을 때까지 기다리기 위한 블로킹 방식
                val markdownContent = getCompleteResponse(notePrompt)

                // 내용이 비어있는지 확인
                val finalContent = if (markdownContent.isBlank()) {
                    Log.e("GenieChat", "마크다운 내용이 비어있음")
                    "# 대화 요약\n\n대화 내용을 요약하는데 실패했습니다. 다시 시도해주세요."
                } else {
                    Log.d("GenieChat", "마크다운 내용: $markdownContent")
                    markdownContent
                }

                // 노트 생성 완료 후 노트 표시 화면으로 이동
                runOnUiThread {
                    val intent = Intent(this@GenieConversationActivity, MarkdownNoteActivity::class.java)
                    intent.putExtra(MarkdownNoteActivity.EXTRA_MARKDOWN_CONTENT, finalContent)
                    intent.putExtra(MarkdownNoteActivity.EXTRA_TITLE, "대화 요약")
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e("GenieChat", "노트 생성 오류: ${e.message}", e)

                runOnUiThread {
                    Toast.makeText(this@GenieConversationActivity,
                        "노트 생성 중 오류가 발생했습니다: ${e.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 응답을 완전히 받을 때까지 기다리는 블로킹 메서드
    private fun getCompleteResponse(prompt: String): String {
        val responseBuilder = StringBuilder()
        val responseLock = CountDownLatch(1)
        var isComplete = false

        genieWrapper.getResponseForPrompt(prompt, object : StringCallback {
            override fun onNewString(response: String) {
                responseBuilder.append(response)
                Log.d("GenieChat", "응답 토큰 받음: $response")

                // 응답이 완료되었는지 확인하는 로직
                // 예: 응답의 마지막에 특정 패턴이 있는지 또는 시간 초과 등
                if (response.contains("</response>") || response.contains("END_OF_RESPONSE") || response.endsWith(".")) {
                    isComplete = true
                    responseLock.countDown()
                }
            }
        })

        // 응답 완료를 기다리거나 10초 후 시간 초과
        try {
            // 최대 10초 대기
            if (!responseLock.await(15, TimeUnit.SECONDS) && !isComplete) {
                // 시간 초과 시 현재까지 받은 내용 사용
                Log.w("GenieChat", "응답 대기 시간 초과, 현재까지 받은 내용 사용")
            }
        } catch (e: InterruptedException) {
            Log.e("GenieChat", "응답 대기 중 인터럽트", e)
        }

        return responseBuilder.toString()
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
