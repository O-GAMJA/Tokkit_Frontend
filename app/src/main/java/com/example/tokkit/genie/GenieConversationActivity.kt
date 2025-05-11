// ========= Genie 대화 화면 Activity =========
package com.example.tokkit.genie

import android.os.Bundle
import android.system.Os
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tokkit.R
import com.example.tokkit.databinding.ActivityGenieChatBinding
import java.nio.file.Paths
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class GenieConversationActivity : AppCompatActivity() {

    private lateinit var binding:ActivityGenieChatBinding
    private val messages = ArrayList<ChatMessage>(1000)
    private lateinit var adapter: MessageRecyclerViewAdapter

    companion object {
        private const val WELCOME_MESSAGE = "안녕하세요! 무엇을 도와드릴까요?"
        const val KEY_HTP_CONFIG = "htp_config_path"
        const val KEY_MODEL_NAME = "model_dir_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGenieChatBinding .inflate(layoutInflater)
        setContentView(binding.root)

        // RecyclerView 설정
        adapter = MessageRecyclerViewAdapter(this, messages)
        binding.chatRecyclerView.adapter = adapter
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)

        // 뒤로가기 버튼 이벤트
        binding.btnBack.setOnClickListener {
            finish()
        }

        try {
            // QNN 라이브러리 경로 설정
            val nativeLibPath = applicationContext.applicationInfo.nativeLibraryDir
            Os.setenv("ADSP_LIBRARY_PATH", nativeLibPath, true)
            Os.setenv("LD_LIBRARY_PATH", nativeLibPath, true)

            // Intent에서 설정 정보 가져오기
            val bundle = intent.extras
            if (bundle == null) {
                Log.e("GenieChat", "Error getting additional info from bundle.")
                Toast.makeText(this, "설정 정보를 가져오는데 실패했습니다.", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            val htpConfigPath = bundle.getString(KEY_HTP_CONFIG)
            val modelName = bundle.getString(KEY_MODEL_NAME)
            val externalCacheDir = this.externalCacheDir?.absolutePath
            val modelDir = Paths.get(externalCacheDir, "models", modelName).toString()

            // 모델 로드
            val genieWrapper = GenieWrapper(modelDir, htpConfigPath)
            Log.i("GenieChat", "$modelName 모델이 로드되었습니다.")

            // 환영 메시지 추가
            messages.add(ChatMessage(WELCOME_MESSAGE, MessageSender.BOT))

            // 사용자 입력 처리 및 AI 응답 받기
            binding.sendButton.setOnClickListener {
                val userMessage = binding.userInput.text.toString()
                if (userMessage.isNotEmpty()) {
                    // 사용자 메시지 입력창 초기화
                    binding.userInput.setText("")

                    // 대화 목록에 사용자 메시지 추가
                    adapter.addMessage(ChatMessage(userMessage, MessageSender.USER))
                    adapter.notifyItemInserted(adapter.itemCount - 1)

                    // 마지막 메시지로 스크롤
                    val botResponseMsgIndex = adapter.itemCount
                    binding.chatRecyclerView.smoothScrollToPosition(botResponseMsgIndex)

                    // 백그라운드에서 AI 응답 생성
                    val service = Executors.newSingleThreadExecutor()
                    service.execute {
                        genieWrapper.getResponseForPrompt(userMessage, object : StringCallback {
                            override fun onNewString(response: String) {
                                runOnUiThread {
                                    // 응답 토큰 업데이트
                                    adapter.updateBotMessage(response)
                                    adapter.notifyItemChanged(botResponseMsgIndex)
                                }
                            }
                        })
                    }

                    // 마지막 메시지로 스크롤
                    binding.chatRecyclerView.scrollToPosition(adapter.itemCount - 1)
                }
            }

        } catch (e: Exception) {
            Log.e("GenieChat", "Error during conversation: ${e}")
            Toast.makeText(this, "오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}