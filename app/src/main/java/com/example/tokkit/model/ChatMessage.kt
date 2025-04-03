package com.example.tokkit.model

data class ChatMessage(
    val message: String,
    val isUser: Boolean // true면 사용자, false면 AI
)
