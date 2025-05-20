package com.example.tokkit.model

data class Comment(
    val username: String,
    val time: String,
    val content: String,
    val likeCount: Int = 0,
    val commentId: Long
)