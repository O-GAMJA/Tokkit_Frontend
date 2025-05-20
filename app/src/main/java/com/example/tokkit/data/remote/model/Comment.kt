package com.example.tokkit.data.remote.model

data class CommentPageResponse(
    val comments: List<CommentResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Int,
    val totalPages: Int,
    val last: Boolean
)

data class CommentResponse(
    val commentId: Long,
    val parentId: Long,
    val writer: String,
    val content: String,
    val emojis: Map<String, EmojiReaction>
)

data class EmojiReaction(
    val count: Int,
    val reactedByCurrentUser: Boolean
)

data class CommentRequest(
    val content: String,
    val parentId: Long? = null  // null이 기본값( = 일반 댓글)
)