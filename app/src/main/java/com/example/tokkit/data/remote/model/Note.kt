package com.example.tokkit.data.remote.model

data class Note(
    val id: String,
    val title: String?,
    val content: String?,
    val imageUrl: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val emojiStatus: EmojiStatus,
    val bookmarkStatus: BookmarkStatus?,
    val tags: List<String>?
)

data class NoteListResult(
    val notes: List<Note>,
    val paginationInfo: PaginationInfo
)

data class PaginationInfo(
    val page: Int,
    val size: Int,
    val totalElements: Int,
    val totalPages: Int,
    val isLast: Boolean
)


data class EmojiStatus(
    val count: Map<String, Int>,
    val clicked: Map<String, Boolean>
)

data class EmojiRequest(
    val emojiType: String
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

data class NoteCreateRequest(
    val id: String,
    val title: String,
    val content: String,
    val isPublic: Boolean,
    val directoryName: String,
    val directoryId: Int? = null,
    val imageUrl: String,
    val conversationLog: String,
    val stage: String = "STAGE0"
    )

data class NoteCreateResponse(
    val total_note_chunks: Int,
    val total_note_count: Int
)

// 이미지 생성 요청 모델
data class ImageGenerationRequest(
    val noteContent: String,
    val style: String
)

// 이미지 생성 응답 모델
data class ImageGenerationResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: ImageResult
)

data class ImageResult(
    val imageUrl: String
)

data class DirectoryTreeResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: List<Directory>
)

data class Directory(
    val name: String,
    val notes: List<Note>,
    val children: List<Directory>,
    val directory_id: Int
)
