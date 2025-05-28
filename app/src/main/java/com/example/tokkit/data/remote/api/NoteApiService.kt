package com.example.tokkit.data.remote.api

import com.example.tokkit.data.remote.model.*
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

    // 유사한 노트 조회 api
    @GET("/search/similarNotes")
    suspend fun getSimilarNotes(
        @Query("noteId") noteId: String,
        @Query("size") size: Int = 5
    ): ApiResponse<SimilarNoteResponse>

    // 추천 노트 조회 api
    @GET("/search/recommendations")
    suspend fun getRecommendedNotes(
        @Query("memberId") memberId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): ApiResponse<SearchResponse>

    // 이모지 추가 api
    @POST("/notes/{noteId}/emoji")
    suspend fun addEmoji(
        @Path("noteId") noteId: String,
        @Body emoji: EmojiRequest
    ): ApiResponse<Note>

    // 이모지 삭제 api
    @HTTP(method = "DELETE", path = "/notes/{noteId}/emoji", hasBody = true)
    suspend fun removeEmoji(
        @Path("noteId") noteId: String,
        @Body emoji: EmojiRequest
    ): ApiResponse<Note>

    // 북마크 달기 api
    @POST("/notes/{noteId}/bookmark")
    suspend fun addBookmark(
        @Path("noteId") noteId: String
    ): ApiResponse<BookmarkResponse>

    // 북마크 해제 api
    @DELETE("/notes/{noteId}/bookmark")
    suspend fun removeBookmark(
        @Path("noteId") noteId: String
    ): ApiResponse<BookmarkResponse>

    // 댓글 목록 조회 api
    @GET("/comments/{noteId}")
    suspend fun getComments(
        @Path("noteId") noteId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): ApiResponse<CommentPageResponse>

    // 댓글 작성 api
    @POST("/comments/{noteId}")
    suspend fun postComment(
        @Path("noteId") noteId: String,
        @Body commentRequest: CommentRequest
    ): ApiResponse<Unit>

    // 댓글 수정 api
    @PATCH("/comments/{commentId}")
    suspend fun updateComment(
        @Path("commentId") commentId: Long,
        @Body content: Map<String, String>
    ): ApiResponse<Unit>

    // 댓글 삭제 api
    @DELETE("/comments/{commentId}")
    suspend fun deleteComment(
        @Path("commentId") commentId: Long
    ): ApiResponse<Unit>

    // 댓글 이모지 추가 api
    @POST("/comments/{commentId}/emoji")
    suspend fun addEmoji(
        @Path("commentId") commentId: Long,
        @Body body: Map<String, String> // e.g., {"emojiName": "LIKE"}
    ): ApiResponse<Unit>

    // 댓글 이모지 삭제 api
    @HTTP(method = "DELETE", path = "/comments/{commentId}/emoji", hasBody = true)
    suspend fun removeEmoji(
        @Path("commentId") commentId: Long,
        @Body body: Map<String, String> // e.g., {"emojiName": "LIKE"}
    ): ApiResponse<Unit>

    // 노트 저장 api
    @POST("/notes")
    @Headers("Content-Type: application/json")
    suspend fun createNote(
        @Query("memberId") memberId: Long,
        @Query("batch") batch: Boolean,
        @Body notes: List<NoteCreateRequest>
    ): ApiResponse<NoteCreateResponse>

    // 유저가 가진 태그 개수 조회(버블 차트) api
    @GET("/notes/tags")
    suspend fun getTagsByMemberId(
        @Query("memberId") memberId: Long
    ): ApiResponse<List<TagCount>>

    // 특정 태그로 노트 목록 조회(검색) api
    @GET("/notes/tags/{tagName}")
    suspend fun getNotesByTag(
        @Path("tagName") tagName: String,
        @Query("memberId") memberId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): ApiResponse<NoteListResult>

    // 음성강의 조회 api
    @GET("/{noteId}")
    suspend fun getLectureAudio(@Path("noteId") noteId: String): Response<ApiResponse<LectureAudioResponse>>
}
