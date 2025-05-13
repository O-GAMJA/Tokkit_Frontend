package com.example.tokkit.data.remote.model

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val imageUrl: String?,
    val createdAt: String,
    val updatedAt: String,
    val emojiStatus: EmojiStatus,
    val bookmarkStatus: BookmarkStatus,
    val tags: List<String>
)

data class EmojiStatus(
    val count: Map<String, Int>,
    val clicked: Map<String, Boolean>
)

data class BookmarkStatus(
    val count: Int,
    val clicked: Boolean
)

data class ApiResponse<T>(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: T
)
