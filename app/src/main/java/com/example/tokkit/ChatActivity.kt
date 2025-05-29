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
import android.speech.tts.UtteranceProgressListener
import android.system.Os
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.tokkit.databinding.ActivityChatBinding
import com.example.tokkit.databinding.ActivityGenieChatBinding
import com.example.tokkit.genie.ChatMessage
import com.example.tokkit.genie.ConversationManager
import com.example.tokkit.genie.GenieConversationActivity
import com.example.tokkit.genie.GenieWrapper
import com.example.tokkit.genie.MarkdownNoteActivity
import com.example.tokkit.genie.MessageRecyclerViewAdapter
import com.example.tokkit.genie.MessageSender
import com.example.tokkit.genie.StringCallback
import com.example.tokkit.genie.MarkdownPromptHandler
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths
import java.util.ArrayList
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import com.example.tokkit.util.CustomToastUtil

class ChatActivity : AppCompatActivity(), ConversationManager.ConversationChangeListener {
    private lateinit var binding: ActivityChatBinding
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private lateinit var genieWrapper: GenieWrapper
    private lateinit var adapter: MessageRecyclerViewAdapter


    private val messages = ArrayList<ChatMessage>(1000)
    private var isListening = false
    private var fullResponse = StringBuilder()
    private var responseTimeoutHandler = Handler(Looper.getMainLooper())
    private var responseTimeoutRunnable: Runnable? = null

    private var isTtsPlaying = false
    private var ttsQueue = mutableListOf<String>()
    private var isProcessingTTS = false
    private val animationHandler = Handler(Looper.getMainLooper())

    // 응답 완료 감지를 위한 변수
    private var isResponseComplete = false
    private var responseCompleteHandler = Handler(Looper.getMainLooper())
    private var responseCompleteRunnable: Runnable? = null

    // TTS 애니메이션 관련 변수
    private var isSpeaking = false
    private var currentlyPlayingTTS = false // 실제 TTS 재생 상태

    // 문장 단위 TTS 처리를 위한 버퍼
    private val sentenceBuffer = StringBuilder()

    private val markdownPromptHandler = MarkdownPromptHandler()



    companion object {
        private const val WELCOME_MESSAGE = "안녕하세요! 무엇을 도와드릴까요?"
        private const val REQUEST_RECORD_AUDIO = 100
        private const val TTS_UTTERANCE_ID = "TTS_CHAT_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent에서 OCR 텍스트 가져오기
        val ocrText = intent.getStringExtra("OCR_TEXT")
        Log.d("ChatActivity", "OCR 텍스트 받음: ${if (ocrText.isNullOrEmpty()) "없음" else "있음"}")

        // 저장된 대화 내용을 먼저 로드
        Log.d("ChatActivity", "대화 내용 로드 시작")
        ConversationManager.loadConversation(this)

        // Genie 초기화
        initializeGenie()

        // OCR 텍스트가 있는 경우 Genie에 설정
        if (!ocrText.isNullOrEmpty()) {
            if (::genieWrapper.isInitialized) {
                genieWrapper.setOcrText(ocrText)
                Log.d("ChatActivity", "OCR 텍스트 설정 완료")

                // 기존 대화 내용 확인
                val existingMessages = ConversationManager.getAllMessages()
                val hasOcrMessage = existingMessages.any {
                    it.mSender == MessageSender.BOT &&
                            it.mMessage.contains("학습 노트 내용을 참고하여")
                }

                // OCR 참고 메시지가 없으면 추가 (내용은 포함하지 않음)
                if (!hasOcrMessage) {
                    val message = ChatMessage("학습 노트 내용을 참고하여 답변드리겠습니다!", MessageSender.BOT)
                    ConversationManager.addMessage(message)
                    Log.d("ChatActivity", "OCR 참고 메시지 추가")
                }
            }
        }

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
        }

        // 마이크 버튼 클릭 - 음성 인식 시작
        binding.btnMic.setOnClickListener {
            startSTT()
        }

        // 노트 생성 버튼
        binding.btnCreateNote.setOnClickListener {
            createMarkdownNote()
        }

        // 기존 대화 내용 처리
        val existingMessages = ConversationManager.getAllMessages()
        Log.d("ChatActivity", "기존 메시지 개수: ${existingMessages.size}")

        if (existingMessages.isEmpty()) {
            Log.d("ChatActivity", "새로운 대화 시작 - 환영 메시지 추가")
            val welcomeMessage = ChatMessage(WELCOME_MESSAGE, MessageSender.BOT)
            ConversationManager.addMessage(welcomeMessage)
        } else {
            Log.d("ChatActivity", "기존 대화 로드됨: ${existingMessages.size}개 메시지")
            // ConversationManager에서 기존 대화 내용 로드
            loadMessagesFromManager()
        }

        // 초기 캐릭터 이미지 설정
        setupCharacterAnimation()
    }
    private fun setupCharacterAnimation() {
        // 기본 정적 이미지 표시
        binding.ivCharacter.visibility = View.VISIBLE
        binding.ivCharacterGif.visibility = View.GONE
    }

    private fun createMarkdownNote() {
        // 노트 생성 시작 시 TTS 중지
        stopCurrentTts()

        // 로딩 오버레이 표시
        showNoteLoadingOverlay()

        val conversation = ConversationManager.getConversationText()
        Log.d("ChatActivity", "대화 내용: $conversation")

        val notePrompt = markdownPromptHandler.getPromptForNoteGeneration(conversation)

        val service = Executors.newSingleThreadExecutor()
        service.execute {
            try {
                val markdownContent = getCompleteResponse(notePrompt)

                val finalContent = if (markdownContent.isBlank()) {
                    Log.e("ChatActivity", "마크다운 내용이 비어있음")
                    "# 대화 요약\n\n대화 내용을 요약하는데 실패했습니다. 다시 시도해주세요."
                } else {
                    Log.d("ChatActivity", "마크다운 내용: $markdownContent")
                    markdownContent
                }

                // 마크다운 내용에서 제목 추출
                val noteTitle = com.example.tokkit.util.MarkdownUtil.extractTitleFromMarkdown(finalContent)
                Log.d("ChatActivity", "추출된 제목: $noteTitle")

                runOnUiThread {
                    // 로딩 오버레이 숨기기
                    hideNoteLoadingOverlay()

                    val intent = Intent(this, MarkdownNoteActivity::class.java)
                    intent.putExtra(MarkdownNoteActivity.EXTRA_MARKDOWN_CONTENT, finalContent)
                    intent.putExtra(MarkdownNoteActivity.EXTRA_TITLE,noteTitle)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "노트 생성 오류: ${e.message}", e)
                runOnUiThread {
                    // 로딩 오버레이 숨기기
                    hideNoteLoadingOverlay()
                    CustomToastUtil.showToast(
                        context = this,
                        message = "노트 생성 중 오류가 발생했습니다: ${e.message}",
                        iconResId = R.drawable.ic_bot
                    )
                }
            }
        }
    }

    private fun getCompleteResponse(prompt: String): String {
        val responseBuilder = StringBuilder()
        val responseLock = CountDownLatch(1)
        var isComplete = false

        genieWrapper.getResponseForPrompt(prompt, object : StringCallback {
            override fun onNewString(response: String) {
                responseBuilder.append(response)
                Log.d("ChatActivity", "응답 토큰 받음: $response")

                if (response.contains("</response>") || response.contains("END_OF_RESPONSE") || response.endsWith(".")) {
                    isComplete = true
                    responseLock.countDown()
                }
            }
        })

        try {
            if (!responseLock.await(15, TimeUnit.SECONDS) && !isComplete) {
                Log.w("ChatActivity", "응답 대기 시간 초과, 현재까지 받은 내용 사용")
            }
        } catch (e: InterruptedException) {
            Log.e("ChatActivity", "응답 대기 중 인터럽트", e)
        }

        return responseBuilder.toString()
    }


    private fun initializeTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US

                // TTS 진행 상태 리스너 설정
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d("ChatActivity", "TTS 시작: $utteranceId")
                        currentlyPlayingTTS = true
                        isTtsPlaying = true

                        // 액티비티 상태 확인 후 애니메이션 시작
                        if (!isDestroyed && !isFinishing) {
                            runOnUiThread {
                                if (!isDestroyed && !isFinishing) {
                                    startTTSAnimation()
                                }
                            }
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.d("ChatActivity", "TTS 완료: $utteranceId")
                        currentlyPlayingTTS = false

                        if (!isDestroyed && !isFinishing) {
                            runOnUiThread {
                                if (!isDestroyed && !isFinishing) {
                                    // 다음 TTS 처리
                                    processNextTTSInQueue()

                                    // 큐가 비어있고 더 이상 처리할 TTS가 없으면 애니메이션 중지
                                    if (ttsQueue.isEmpty() && !isProcessingTTS) {
                                        isTtsPlaying = false
                                        stopTTSAnimation()
                                    }
                                }
                            }
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        Log.e("ChatActivity", "TTS 오류: $utteranceId")
                        currentlyPlayingTTS = false
                        isTtsPlaying = false

                        if (!isDestroyed && !isFinishing) {
                            runOnUiThread {
                                if (!isDestroyed && !isFinishing) {
                                    stopTTSAnimation()
                                    processNextTTSInQueue()
                                }
                            }
                        }
                    }
                })
            } else {
                Log.e("ChatActivity", "TTS 초기화 실패: $status")
            }
        }
    }

    // TTS 중지
    private fun stopCurrentTts() {
        if (::tts.isInitialized) {
            Log.d("ChatActivity", "TTS 중지 시도: isTtsPlaying = $isTtsPlaying")

            // TTS 완전히 중지
            tts.stop()

            // 큐 비우기
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                tts.speak("", TextToSpeech.QUEUE_FLUSH, null, "stop_utterance")
            }

            // 상태 초기화
            isTtsPlaying = false
            currentlyPlayingTTS = false
            ttsQueue.clear()
            isProcessingTTS = false
            isResponseComplete = false

            // 애니메이션 즉시 중지
            stopTTSAnimation()

            Log.d("ChatActivity", "TTS 중지 완료")
        }

        // 대기 중인 응답 타임아웃도 취소
        responseCompleteRunnable?.let {
            responseCompleteHandler.removeCallbacks(it)
            Log.d("ChatActivity", "응답 타임아웃 취소됨")
        }

        // 문장 버퍼도 클리어
        sentenceBuffer.clear()
    }

    private fun startTTSAnimation() {
        // 액티비티가 파괴되었거나 finishing 상태인지 확인
        if (isDestroyed || isFinishing) {
            Log.w("ChatActivity", "액티비티가 파괴된 상태에서 TTS 애니메이션 시작 시도 - 무시됨")
            return
        }

        // 이미 애니메이션이 실행 중이면 중복 실행 방지
        if (isSpeaking) {
            Log.d("ChatActivity", "TTS 애니메이션이 이미 실행 중")
            return
        }

        isSpeaking = true

        // 정적 이미지 숨기고 GIF 표시
        binding.ivCharacter.visibility = View.GONE
        binding.ivCharacterGif.visibility = View.VISIBLE

        // GIF 로드 및 재생
        try {
            if (!isDestroyed && !isFinishing) {
                Glide.with(this)
                    .asGif()
                    .load(R.drawable.rabbit_animation)
                    .into(binding.ivCharacterGif)

                Log.d("ChatActivity", "TTS 애니메이션 시작")
            }
        } catch (e: Exception) {
            Log.e("ChatActivity", "Glide 로딩 오류: ${e.message}")
            // 오류 발생 시 상태 복원
            binding.ivCharacterGif.visibility = View.GONE
            binding.ivCharacter.visibility = View.VISIBLE
            isSpeaking = false
        }
    }

    private fun stopTTSAnimation() {
        if (!isSpeaking) {
            return // 이미 중지된 상태
        }

        isSpeaking = false

        // 액티비티 상태 확인 후 애니메이션 복원
        if (!isDestroyed && !isFinishing) {
            // 즉시 정적 이미지로 복원 (딜레이 제거)
            binding.ivCharacterGif.visibility = View.GONE
            binding.ivCharacter.visibility = View.VISIBLE
            Log.d("ChatActivity", "TTS 애니메이션 종료")
        }
    }


    // TTS 큐에 문장 추가하고 처리
    private fun addToTTSQueue(text: String) {
        if (text.trim().isNotEmpty()) {
            ttsQueue.add(text.trim())
            Log.d("ChatActivity", "TTS 큐에 추가: $text")

            if (!isProcessingTTS && !currentlyPlayingTTS) {
                processNextTTSInQueue()
            }
        }
    }


    // TTS 큐에서 다음 문장 처리
    private fun processNextTTSInQueue() {
        if (ttsQueue.isNotEmpty() && !currentlyPlayingTTS) {
            isProcessingTTS = true
            val nextText = ttsQueue.removeAt(0)

            Log.d("ChatActivity", "TTS 재생 시작: $nextText")

            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "TTS_${System.currentTimeMillis()}"

            // TTS 재생 시작
            tts.speak(nextText, TextToSpeech.QUEUE_ADD, params)
        } else {
            isProcessingTTS = false

            // 모든 TTS가 완료되고 응답도 완료되었으면 애니메이션 중지
            if (isResponseComplete && ttsQueue.isEmpty() && !currentlyPlayingTTS) {
                isTtsPlaying = false
                stopTTSAnimation()
            }
        }
    }


    private fun speakWithAnimation(text: String) {
        if (::tts.isInitialized) {
            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = TTS_UTTERANCE_ID

            tts.speak(text, TextToSpeech.QUEUE_ADD, params)
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
                CustomToastUtil.showToast(
                    context = this@ChatActivity,
                    message = "음성 인식 오류 발생: $error",
                    iconResId = R.drawable.ic_bot
                )

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
        // STT 시작 시 TTS 중지
        stopCurrentTts()

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
                CustomToastUtil.showToast(
                    context = this,
                    message = "음성 인식 시작 실패",
                    iconResId = R.drawable.ic_bot
                )

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
        // 새로운 질문 시작 시 기존 TTS 중지
        stopCurrentTts()

        // ConversationManager를 통해 사용자 메시지 추가
        val userMessage = ChatMessage(message, MessageSender.USER)
        ConversationManager.addMessage(userMessage)

        // 초기화
        sentenceBuffer.clear()
        fullResponse.clear()
        ttsQueue.clear()
        isResponseComplete = false
        isProcessingTTS = false
        currentlyPlayingTTS = false

        // 응답 생성 요청
        if (::genieWrapper.isInitialized) {
            val executor = Executors.newSingleThreadExecutor()
            executor.execute {
                genieWrapper.getResponseForPrompt(message, object : StringCallback {
                    override fun onNewString(response: String) {
                        runOnUiThread {
                            // 누적 응답에 새 응답 추가
                            fullResponse.append(response)
                            sentenceBuffer.append(response)

                            // ConversationManager를 통해 봇 메시지 업데이트
                            ConversationManager.updateLastBotMessage(response)

                            // 문장 단위로 TTS 큐에 추가
                            val bufferStr = sentenceBuffer.toString()
                            val sentenceEndPattern = Regex("[.!?]\\s*")

                            if (sentenceEndPattern.containsMatchIn(bufferStr)) {
                                val sentences = sentenceEndPattern.split(bufferStr)

                                if (sentences.size > 1) {
                                    for (i in 0 until sentences.size - 1) {
                                        val sentence = sentences[i].trim()
                                        if (sentence.isNotBlank()) {
                                            addToTTSQueue(sentence)
                                        }
                                    }

                                    // 버퍼 초기화 후 마지막 미완성 문장만 유지
                                    sentenceBuffer.clear()
                                    sentenceBuffer.append(sentences.last())
                                }
                            }

                            // 응답 완료 감지를 위한 타이머 리셋
                            responseCompleteRunnable?.let { responseCompleteHandler.removeCallbacks(it) }
                            responseCompleteRunnable = Runnable {
                                // 응답이 완료된 것으로 간주
                                isResponseComplete = true
                                val lastSentence = sentenceBuffer.toString().trim()
                                if (lastSentence.isNotBlank()) {
                                    addToTTSQueue(lastSentence)
                                    sentenceBuffer.clear()
                                }

                                // TTS 큐가 비어있고 현재 재생 중이 아니면 애니메이션 중지
                                if (ttsQueue.isEmpty() && !currentlyPlayingTTS && !isProcessingTTS) {
                                    isTtsPlaying = false
                                    stopTTSAnimation()
                                }
                            }
                            responseCompleteHandler.postDelayed(responseCompleteRunnable!!, 1500) // 타이머 약간 증가
                        }
                    }
                })
            }
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
                    CustomToastUtil.showToast(
                        context = this,
                        message = "지원되지 않는 디바이스입니다",
                        iconResId = R.drawable.ic_bot
                    )
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
            CustomToastUtil.showToast(
                context = this,
                message = "Genie 초기화 중 오류 발생: ${e.message}",
                iconResId = R.drawable.ic_bot
            )
        }
    }

    private fun loadMessagesFromManager() {
        messages.clear()
        messages.addAll(ConversationManager.getAllMessages())

        Log.d("ChatActivity", "loadMessagesFromManager(): 메시지 ${messages.size}개 로딩됨")
        for ((index, message) in messages.withIndex()) {
            Log.d("ChatActivity", "[$index] ${if (message.isMessageFromUser()) "USER" else "BOT"}: ${message.getMessage()}")
        }
    }

    private fun startGenieConversation() {
        // Genie 대화 시작 시 TTS 완전히 중지 및 정리
        stopCurrentTts()

        // 추가적인 TTS 정리 - 모든 콜백도 무효화
        if (::tts.isInitialized) {
            tts.stop()
            tts.setOnUtteranceProgressListener(null) // 리스너 제거
        }

        // 애니메이션도 즉시 중지
        isSpeaking = false
        animationHandler.removeCallbacksAndMessages(null)

        // 액티비티 전환 전에 먼저 대화 저장
        ConversationManager.saveConversation(this)

        try {
            val externalDir = externalCacheDir?.absolutePath ?: ""

            // HTP 설정 파일 경로 결정
            val socModel = android.os.Build.SOC_MODEL
            val htpConfig = when (socModel) {
                "SM8750" -> "qualcomm-snapdragon-8-elite.json"
                "SM8650" -> "qualcomm-snapdragon-8-gen3.json"
                "QCS8550" -> "qualcomm-snapdragon-8-gen2.json"
                else -> {
                    CustomToastUtil.showToast(
                        context = this,
                        message = "지원되지 않는 디바이스입니다",
                        iconResId = R.drawable.ic_bot
                    )
                    return
                }
            }

            val htpConfigPath = Paths.get(externalDir, "htp_config", htpConfig).toString()

            val intent = Intent(this, GenieConversationActivity::class.java).apply {
                putExtra(GenieConversationActivity.KEY_HTP_CONFIG, htpConfigPath)
                putExtra(GenieConversationActivity.KEY_MODEL_NAME, "llama3_2_3b")

                // OCR 텍스트를 현재 액티비티의 인텐트에서 가져와 새 인텐트에 추가
                val ocrText = this@ChatActivity.intent.getStringExtra("OCR_TEXT")
                if (!ocrText.isNullOrEmpty()) {
                    putExtra("OCR_TEXT", ocrText)
                }
            }

            startActivity(intent)

            // 약간의 딜레이 후 finish() 호출
            Handler(Looper.getMainLooper()).postDelayed({
                finish()
            }, 100) // 100ms 딜레이

        } catch (e: Exception) {
            Log.e("ChatActivity", "Genie 대화 시작 오류: ${e.message}", e)
            CustomToastUtil.showToast(
                context = this,
                message = "Genie 대화 시작 중 오류 발생: ${e.message}",
                iconResId = R.drawable.ic_bot
            )
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
        runOnUiThread {
            messages.clear()
            messages.addAll(updatedMessages)

            Log.d("ChatActivity", "onConversationChanged(): 메시지 ${messages.size}개 로딩됨")
            for ((index, message) in messages.withIndex()) {
                Log.d("ChatActivity", "[$index] ${if (message.isMessageFromUser()) "USER" else "BOT"}: ${message.getMessage()}")
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeRecognizer()
        } else {
            CustomToastUtil.showToast(
                context = this,
                message = "음성 인식을 위해 마이크 권한이 필요합니다",
                iconResId = R.drawable.ic_bot
            )
        }
    }

    // 노트 생성 로딩 오버레이 표시
    private fun showNoteLoadingOverlay() {
        // 로딩 오버레이 표시
        binding.noteLoadingOverlay.visibility = View.VISIBLE

        // 문서 애니메이션 시작
        binding.lottieDocAnimation.playAnimation()
    }

    // 노트 생성 로딩 오버레이 숨기기
    private fun hideNoteLoadingOverlay() {
        // 로딩 오버레이 숨기기
        binding.noteLoadingOverlay.visibility = View.GONE

        // 문서 애니메이션 중지
        binding.lottieDocAnimation.cancelAnimation()
    }

    override fun onPause() {
        super.onPause()
        // 액티비티가 중지될 때 TTS도 중지
        stopCurrentTts()

        // 활동이 중지될 때 대화 상태 저장
        ConversationManager.saveConversation(this)
    }

    override fun onDestroy() {
        // TTS 완전히 정리
        stopCurrentTts()

        // ConversationManager 리스너 제거
        ConversationManager.removeListener(this)

        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }

        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }

        // 핸들러 정리
        responseTimeoutHandler.removeCallbacksAndMessages(null)
        animationHandler.removeCallbacksAndMessages(null)
        responseCompleteHandler.removeCallbacksAndMessages(null)

        super.onDestroy()
    }
}