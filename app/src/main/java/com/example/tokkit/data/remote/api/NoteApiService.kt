package com.example.tokkit.data.remote.api

import com.example.tokkit.data.remote.model.ApiResponse
import com.example.tokkit.data.remote.model.EmojiRequest
import com.example.tokkit.data.remote.model.Note
import com.example.tokkit.data.remote.model.NoteCreateRequest
import com.example.tokkit.data.remote.model.NoteCreateResponse
import com.example.tokkit.data.remote.model.NoteListResult
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.HTTP
import retrofit2.http.Query

interface NoteApiService {
    // 회원의 모든 노트 조회 api
    @GET("/notes/{memberId}/all")
    suspend fun getAllNotes(
        @Path("memberId") memberId: Long,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): ApiResponse<NoteListResult>

    // 노트 상세 조회 api
    @GET("/notes/{noteId}")
    suspend fun getNoteById(
        @Path("noteId") noteId: String
    ): ApiResponse<Note>

    // 노트 삭제 api
    @DELETE("notes/{noteId}")
    suspend fun deleteNote(@Path("noteId") noteId: String): Response<ApiResponse<Unit>>

    // 노트 수정 api
    @PATCH("notes/{noteId}")
    @Headers("Content-Type: application/merge-patch+json")
    suspend fun updateNote(
        @Path("noteId") noteId: String,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ApiResponse<Unit>>

    // 이모지 추가
    @POST("/notes/{noteId}/emoji")
    suspend fun addEmoji(
        @Path("noteId") noteId: String,
        @Body emoji: EmojiRequest
    ): ApiResponse<Note>

    // 이모지 삭제
    @HTTP(method = "DELETE", path = "/notes/{noteId}/emoji", hasBody = true)
    suspend fun removeEmoji(
        @Path("noteId") noteId: String,
        @Body emoji: EmojiRequest
    ): ApiResponse<Note>

    // 노트 저장 api
    @POST("/notes")
    suspend fun createNote(
        @Query("memberId") memberId: Long,
        @Body notes: List<NoteCreateRequest>
    ): ApiResponse<NoteCreateResponse>
}
