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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tokkit.data.remote.repository.ReviewRepository
import com.example.tokkit.databinding.ActivityReviewSpeakingBinding
import com.example.tokkit.genie.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Executors
import com.example.tokkit.util.CustomToastUtil


class ReviewSpeakingActivity : AppCompatActivity(), ConversationManager.ConversationChangeListener {

        private lateinit var binding: ActivityReviewSpeakingBinding
        private val messages = ArrayList<ChatMessage>(1000)
        private lateinit var adapter: MessageRecyclerViewAdapter
        private lateinit var genieWrapper: GenieWrapper
        private lateinit var speechRecognizer: SpeechRecognizer
        private lateinit var tts: TextToSpeech
        private var fullResponse = StringBuilder()
        private var tempOcrText: String? = null  // OCR 텍스트를 임시 저장할 변수

        // 1초 동안 새 토큰이 없으면 응답 종료로 간주
        private var responseTimeoutHandler = Handler(Looper.getMainLooper())
        private var responseTimeoutRunnable: Runnable? = null

        // 문장 단위 TTS 처리를 위한 버퍼
        private val sentenceBuffer = StringBuilder()

        private val markdownPromptHandler = MarkdownPromptHandler()
        private var isListening = false



        companion object {
            private const val WELCOME_MESSAGE = "안녕하세요! 무엇을 도와드릴까요?"
            const val EXTRA_NOTE_ID = "NOTE_ID"
            const val EXTRA_NOTE_CONTENT = "NOTE_CONTENT"
            const val KEY_HTP_CONFIG = "htp_config_path"
            const val KEY_MODEL_NAME = "model_dir_name"
            private const val REQUEST_RECORD_AUDIO = 100
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityReviewSpeakingBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Intent에서 OCR 텍스트 가져와서 임시 변수에 저장 (아직 설정하지 않음)
            tempOcrText = intent.getStringExtra("OCR_TEXT")

            // 저장된 대화 내용 로드
            ConversationManager.loadConversation(this)

            // RecyclerView 설정
            adapter = MessageRecyclerViewAdapter(this, messages)
            binding.chatRecyclerView.adapter = adapter
            binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)

            // 스와이프 삭제 기능 추가
            setupSwipeToDelete()

            // 삭제 리스너 설정
            adapter.setOnMessageDeleteListener(object : MessageRecyclerViewAdapter.OnMessageDeleteListener {
                override fun onMessageDelete(position: Int) {
                    deleteMessage(position)
                }
            })

            // ConversationManager에 리스너 등록
            ConversationManager.addListener(this)

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
                    CustomToastUtil.showToast(
                        context = this,
                        message = "설정 정보를 가져오지 못했습니다.",
                        iconResId = R.drawable.ic_bot
                    )
                    finish()
                    return
                }

                val htpConfigPath = bundle.getString(KEY_HTP_CONFIG)
                val modelName = bundle.getString(KEY_MODEL_NAME)
                val externalCacheDir = this.externalCacheDir?.absolutePath
                val modelDir = Paths.get(externalCacheDir, "models", modelName).toString()

                // GenieWrapper 초기화 - 이 시점 이후에만 genieWrapper 사용 가능
                genieWrapper = GenieWrapper(modelDir, htpConfigPath)
                Log.i("GenieChat", "$modelName 모델 로드 완료")

                // 퀴즈 모드 ON
                genieWrapper.setQuizMode(true)

                // 노트 내용 세팅
                val noteContent = intent.getStringExtra(EXTRA_NOTE_CONTENT)
                if (!noteContent.isNullOrBlank()) {
                    genieWrapper.setOcrText(noteContent)

                    val message = ChatMessage("Let's begin the quiz based on your note!", MessageSender.BOT)
                    ConversationManager.addMessage(message)

                    // AI가 첫 질문을 시작하도록 유도
                    sendMessage("Please start the quiz")
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

                // 마이크 버튼 클릭 시 STT 시작
                binding.btnMic.setOnClickListener {
                    startSTT()
                }

                // 텍스트 전송 버튼 클릭 리스너 추가
                binding.btnSend.setOnClickListener {
                    val message = binding.etMessage.text.toString().trim()
                    if (message.isNotEmpty()) {
                        sendMessage(message)
                        binding.etMessage.text.clear()
                    }
                }

                // 키보드 엔터키(완료) 눌렀을 때도 전송
                binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                        binding.btnSend.performClick()
                        return@setOnEditorActionListener true
                    }
                    false
                }

                // 대화 복습 종료 버튼
                binding.btnCreateNote.setOnClickListener {
                    val noteId = intent.getStringExtra("NOTE_ID") ?: return@setOnClickListener
                    val content = ConversationManager.getConversationText()

                    showNoteLoadingOverlay()

                    CoroutineScope(Dispatchers.IO).launch {
                        val result = ReviewRepository().submitConversationReview(noteId, content)

                        withContext(Dispatchers.Main) {
                            hideNoteLoadingOverlay()
                            if (result != null) {
                                ConversationManager.clearMessages()
                                ConversationManager.clearSavedConversation(this@ReviewSpeakingActivity)

                                // 결과를 Intent에 담아서 전달
                                val resultIntent = Intent().apply {
                                    putExtra("NEW_STAGE", result.newStage)

                                    // newStage가 COMPLETE인지 확인하여 정수값도 함께 전달
                                    if (result.newStage == "COMPLETE") {
                                        putExtra("NEW_STAGE_INT", 5) // 완료 상태
                                    } else {
                                        // 숫자 단계 추출
                                        val stageInt = result.newStage.filter { it.isDigit() }.toIntOrNull() ?: 0
                                        putExtra("NEW_STAGE_INT", stageInt)
                                    }
                                }

                                setResult(RESULT_OK, resultIntent)
                                finish()
                            } else {
                                CustomToastUtil.showToast(
                                    context = this@ReviewSpeakingActivity,
                                    message = "복습 제출 실패",
                                    iconResId = R.drawable.ic_bot
                                )
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("GenieChat", "에러: ${e}")
                CustomToastUtil.showToast(
                    context = this,
                    message = "초기화 오류: ${e.message}",
                    iconResId = R.drawable.ic_bot
                )
                finish()
            }
        }



        private fun loadMessagesFromManager() {
            messages.clear()
            messages.addAll(ConversationManager.getAllMessages())
            adapter.notifyDataSetChanged()

            Log.d("GenieChat", "loadMessagesFromManager(): 메시지 ${messages.size}개 로딩됨")
            for ((index, message) in messages.withIndex()) {
                Log.d("GenieChat", "[$index] ${if (message.isMessageFromUser()) "USER" else "BOT"}: ${message.getMessage()}")
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
            // ConversationManager를 통해 메시지 추가
            val userMessage = ChatMessage(message, MessageSender.USER)
            ConversationManager.addMessage(userMessage)

            // 문장 버퍼 초기화
            sentenceBuffer.clear()
            fullResponse.clear() // 이전 응답 초기화

            val executor = Executors.newSingleThreadExecutor()
            executor.execute {
                genieWrapper.getResponseForPrompt(message, object : StringCallback {
                    override fun onNewString(response: String) {
                        runOnUiThread {
                            // 누적 응답에 새 응답 추가
                            fullResponse.append(response)
                            sentenceBuffer.append(response)
                            Log.d("GenieResponse", "AI 전체 응답: ${fullResponse.toString().replace("\n", "\\n")}")

                            // ConversationManager를 통해 봇 메시지 업데이트
                            val updatedResponse = ConversationManager.updateLastBotMessage(response)

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

                            // 이전 대기 제거
                            responseTimeoutRunnable?.let { responseTimeoutHandler.removeCallbacks(it) }

                            // 새 대기 설정 - 완전히 응답이 끝났을 때 마지막 미완성 문장 처리
                            responseTimeoutRunnable = Runnable {
                                val lastSentence = sentenceBuffer.toString().trim()
                                if (lastSentence.isNotBlank()) {
                                    tts.speak(lastSentence, TextToSpeech.QUEUE_ADD, null, null)
                                    sentenceBuffer.clear()
                                }
                            }
                            responseTimeoutHandler.postDelayed(responseTimeoutRunnable!!, 500)
                        }
                }
            })
        }
    }

    // ConversationManager.ConversationChangeListener 구현
    override fun onConversationChanged(updatedMessages: List<ChatMessage>) {
        runOnUiThread {
            messages.clear()
            messages.addAll(updatedMessages)
            adapter.notifyDataSetChanged()

            Log.d("GenieChat", "onConversationChanged(): 메시지 ${messages.size}개 로딩됨")
            for ((index, message) in messages.withIndex()) {
                Log.d("GenieChat", "[$index] ${if (message.isMessageFromUser()) "USER" else "BOT"}: ${message.getMessage()}")
            }
        }
    }

    // 대화 복습 종료 로딩 오버레이 표시
    private fun showNoteLoadingOverlay() {
        // 로딩 오버레이 표시
        binding.noteLoadingOverlay.visibility = View.VISIBLE

        // 문서 애니메이션 시작
        binding.lottieDocAnimation.playAnimation()
    }

    // 대화 복습 종료 로딩 오버레이 숨기기
    private fun hideNoteLoadingOverlay() {
        // 로딩 오버레이 숨기기
        binding.noteLoadingOverlay.visibility = View.GONE

        // 문서 애니메이션 중지
        binding.lottieDocAnimation.cancelAnimation()
    }

    private fun setupSwipeToDelete() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                showDeleteConfirmDialog(position)
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.chatRecyclerView)
    }

    private fun showDeleteConfirmDialog(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("메시지 삭제")
            .setMessage("이 메시지를 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                deleteMessage(position)
            }
            .setNegativeButton("취소") { _, _ ->
                // 삭제 취소 시 스와이프 복구
                adapter.notifyItemChanged(position)
            }
            .setCancelable(false)
            .show()
    }

    private fun deleteMessage(position: Int) {
        if (position >= 0 && position < messages.size) {
            // ConversationManager에서 메시지 삭제
            ConversationManager.removeMessageAt(position)

            // UI 갱신은 ConversationChangeListener를 통해 자동으로 처리됨
            // 사용자에게 알림
            Snackbar.make(binding.root, "메시지가 삭제되었습니다", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeRecognizer()
        } else {
            CustomToastUtil.showToast(
                context = this,
                message = "음성 권한이 필요합니다.",
                iconResId = R.drawable.ic_bot
            )
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