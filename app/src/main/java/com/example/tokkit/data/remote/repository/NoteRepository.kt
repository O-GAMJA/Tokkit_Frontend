package com.example.tokkit.data.remote.repository

import android.util.Log
import com.example.tokkit.data.remote.api.NoteApiService
import com.example.tokkit.data.remote.model.BookmarkStatus
import com.example.tokkit.data.remote.model.CommentPageResponse
import com.example.tokkit.data.remote.model.CommentRequest
import com.example.tokkit.data.remote.model.EmojiRequest
import com.example.tokkit.data.remote.model.Note
import com.example.tokkit.data.remote.model.NoteListResult
import com.example.tokkit.data.remote.model.PaginationInfo
import com.example.tokkit.data.remote.model.SimilarNoteItem
import com.example.tokkit.util.RetrofitClient
import retrofit2.HttpException

class NoteRepository {
    private val api: NoteApiService = RetrofitClient.noteApi

    suspend fun getNotes(memberId: Long, page: Int, size: Int): NoteListResult {
        return try {
            val response = api.getAllNotes(memberId, page, size)
            if (response.isSuccess) {
                Log.d("NoteRepository", "파싱 성공: ${response.result.notes.size}")
                response.result
            } else {
                Log.e("NoteRepository", "API 실패: ${response.message}")
                NoteListResult(emptyList(), PaginationInfo(page, size, 0, 0, true))
            }
        } catch (e: Exception) {
            Log.e("NoteRepository", "예외 발생", e)
            NoteListResult(emptyList(), PaginationInfo(page, size, 0, 0, true))
        }
    }


    suspend fun getNoteById(noteId: String): Note? {
        return try {
            val response = api.getNoteById(noteId)
            if (response.isSuccess) response.result else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun toggleEmoji(noteId: String, emojiType: String, isClicked: Boolean): Note? {
        return try {
            val response = if (isClicked) {
                api.removeEmoji(noteId, EmojiRequest(emojiType)) // 삭제 요청
            } else {
                api.addEmoji(noteId, EmojiRequest(emojiType))    // 추가 요청
            }
            if (response.isSuccess) response.result else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun toggleBookmark(noteId: String, isBookmarked: Boolean): BookmarkStatus? {
        return try {
            val response = if (isBookmarked) {
                api.removeBookmark(noteId)
            } else {
                api.addBookmark(noteId)
            }
            if (response.isSuccess) response.result.bookmarkStatusDTO else null
        } catch (e: Exception) {
            Log.e("NoteRepository", "북마크 처리 중 오류", e)
            null
        }
    }

    suspend fun fetchComments(noteId: String, page: Int, size: Int): CommentPageResponse? {
        return try {
            val response = api.getComments(noteId, page, size)
            if (response.isSuccess) response.result else null
        } catch (e: Exception) {
            Log.e("NoteRepository", "댓글 조회 실패", e)
            null
        }
    }

    suspend fun writeComment(noteId: String, content: String, parentId: Long? = null): Boolean {
        return try {
            val response = api.postComment(noteId, CommentRequest(content, parentId))
            response.isSuccess
        } catch (e: Exception) {
            Log.e("NoteRepository", "댓글 작성 실패", e)
            false
        }
    }

    suspend fun updateComment(commentId: Long, newContent: String): Boolean {
        return try {
            val response = api.updateComment(commentId, mapOf("content" to newContent))
            response.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteComment(commentId: Long): Boolean {
        return try {
            val response = api.deleteComment(commentId)
            response.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    suspend fun toggleCommentEmoji(
        commentId: Long,
        emojiName: String,
        isAlreadyReacted: Boolean
    ): Boolean {
        return try {
            val body = mapOf("emojiName" to emojiName)
            val response = if (isAlreadyReacted) {
                api.removeEmoji(commentId, body)
            } else {
                api.addEmoji(commentId, body)
            }
            response.isSuccess
        } catch (e: Exception) {
            Log.e("EmojiToggle", "이모지 토글 실패 → commentId=$commentId, emoji=$emojiName", e)
            false
        }
    }


    suspend fun fetchSimilarNotes(noteId: String, size: Int = 5): List<SimilarNoteItem> {
        Log.d("NoteRepository", "fetchSimilarNotes() called - noteId: $noteId")
        return try {
            val response = api.getSimilarNotes(noteId, size)
            Log.d("NoteRepository", "API response: ${response.result.noteSearchResults.size} items")
            if (response.isSuccess) {
                response.result.noteSearchResults
            } else {
                Log.e("NoteRepository", "API 실패: ${response.message}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("NoteRepository", "유사 노트 API 호출 실패", e)
            emptyList()
        }
    }

    suspend fun getNotesByTag(tagName: String, memberId: Long, page: Int, size: Int): NoteListResult {
        val apiService = RetrofitClient.createService(NoteApiService::class.java)
        val response = apiService.getNotesByTag(tagName, memberId, page, size)
        if (response.isSuccess) {
            return response.result
        } else {
            throw Exception(response.message)
        }
    }


}
