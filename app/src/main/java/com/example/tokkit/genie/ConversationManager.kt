package com.example.tokkit.genie

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.tokkit.model.ChatMessage as AppChatMessage
import java.util.UUID

/**
 * 애플리케이션 전체에서 대화 상태를 관리하는 싱글톤 클래스
 */
object ConversationManager {
    // 대화 메시지 저장
    private val messages = ArrayList<ChatMessage>()

    // 변경 리스너
    private val listeners = mutableListOf<ConversationChangeListener>()

    // 세션 ID를 저장 (앱 실행마다 새로운 ID 생성)
    private var currentSessionId = UUID.randomUUID().toString()

    /**
     * 새 메시지 추가
     */
    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyListeners()
    }

    /**
     * 마지막 봇 메시지 업데이트 (토큰 추가)
     */
    fun updateLastBotMessage(token: String): String {
        if (messages.isEmpty()) {
            val newMsg = ChatMessage(token, MessageSender.BOT)
            messages.add(newMsg)
            notifyListeners()
            return token
        }

        val lastMsg = messages.last()
        return if (lastMsg.mSender == MessageSender.BOT) {
            lastMsg.mMessage += token
            lastMsg.mLength = lastMsg.mMessage.length
            notifyListeners()
            lastMsg.mMessage
        } else {
            val newMsg = ChatMessage(token, MessageSender.BOT)
            messages.add(newMsg)
            notifyListeners()
            token
        }
    }

    /**
     * 모든 메시지 가져오기
     */
    fun getAllMessages(): List<ChatMessage> {
        return messages.toList()
    }

    /**
     * 모든 메시지 지우기
     */
    fun clearMessages() {
        messages.clear()
        notifyListeners()
    }

    /**
     * 새 세션 시작 메서드
     */
    fun startNewSession() {
        messages.clear()
        currentSessionId = UUID.randomUUID().toString()
        notifyListeners()
    }

    /**
     * Genie와 앱 메시지 타입 변환
     */
    fun convertToAppMessage(genieMessage: ChatMessage): AppChatMessage {
        return AppChatMessage(
            genieMessage.mMessage,
            genieMessage.isMessageFromUser()
        )
    }

    fun convertToGenieMessage(appMessage: AppChatMessage): ChatMessage {
        return ChatMessage(
            appMessage.message,
            if (appMessage.isUser) MessageSender.USER else MessageSender.BOT
        )
    }

    /**
     * 리스너 등록
     */
    fun addListener(listener: ConversationChangeListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    /**
     * 리스너 제거
     */
    fun removeListener(listener: ConversationChangeListener) {
        listeners.remove(listener)
    }

    /**
     * 모든 리스너에게 알림
     */
    private fun notifyListeners() {
        listeners.forEach { it.onConversationChanged(messages) }
    }

    /**
     * 대화 내용을 문자열로 변환
     */
    fun getConversationText(): String {
        val builder = StringBuilder()
        for (message in messages) {
            val sender = if (message.isMessageFromUser()) "User" else "Assistant"
            builder.append("$sender: ${message.getMessage()}\n\n")
        }
        return builder.toString()
    }

    // 대화 상태를 영구 저장하는 메서드
    fun saveConversation(context: Context) {
        val sharedPrefs = context.getSharedPreferences("ConversationPrefs", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        // 메시지들을 JSON 문자열로 변환하여 저장
        val messagesJson = messages.map { message ->
            mapOf(
                "message" to message.mMessage,
                "sender" to message.mSender.name
            )
        }

        val jsonString = Gson().toJson(messagesJson)
        editor.putString("conversation_history", jsonString)
        // 세션 ID도 함께 저장
        editor.putString("conversation_session_id", currentSessionId)
        editor.apply()
    }

    // 저장된 대화 상태를 복원하는 메서드
    fun loadConversation(context: Context) {
        val sharedPrefs = context.getSharedPreferences("ConversationPrefs", Context.MODE_PRIVATE)
        val jsonString = sharedPrefs.getString("conversation_history", null)

        if (jsonString != null) {
            val type = object : TypeToken<List<Map<String, String>>>() {}.type
            val savedMessages: List<Map<String, String>> = Gson().fromJson(jsonString, type)

            messages.clear()
            savedMessages.forEach { messageMap ->
                val message = messageMap["message"] ?: ""
                val sender = MessageSender.valueOf(messageMap["sender"] ?: "BOT")
                messages.add(ChatMessage(message, sender))
            }

            notifyListeners()
        }
    }

    // 대화 기록 초기화 메서드
    fun clearSavedConversation(context: Context) {
        val sharedPrefs = context.getSharedPreferences("ConversationPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().remove("conversation_history").remove("conversation_session_id").apply()
    }

    /**
     * 상태 변경을 통지받을 인터페이스
     */
    interface ConversationChangeListener {
        fun onConversationChanged(messages: List<ChatMessage>)
    }
}