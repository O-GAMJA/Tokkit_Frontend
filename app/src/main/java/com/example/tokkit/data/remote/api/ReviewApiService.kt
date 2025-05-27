package com.example.tokkit.data.remote.api

import retrofit2.http.GET
import com.example.tokkit.data.remote.model.ApiResponse
import com.example.tokkit.data.remote.model.ConversationReviewRequest
import com.example.tokkit.data.remote.model.ReviewDetailResponse
import com.example.tokkit.data.remote.model.ReviewStageItem
import com.example.tokkit.data.remote.model.QuizListResponse
import com.example.tokkit.data.remote.model.QuizReviewRequest
import com.example.tokkit.data.remote.model.ReviewResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ReviewApiService {

    // 모든 노트의 복습 단계 조회 api
    @GET("/reviews/all")
    suspend fun getReviewStages(): ApiResponse<List<ReviewStageItem>>

    // 복습 상세 통계 조회 api
    @GET("/reviews/{noteId}")
    suspend fun getReviewDetail(
        @Path("noteId") noteId: String
    ): ApiResponse<ReviewDetailResponse>

    // 대화 복습 제출 api
    @POST("/reviews/conversation/{noteId}")
    suspend fun submitConversationReview(
        @Path("noteId") noteId: String,
        @Body request: ConversationReviewRequest
    ): ApiResponse<ReviewResponse>

    // 퀴즈 조회 api
    @GET("/quiz/{memberId}/{noteId}")
    suspend fun getQuizzes(
        @Path("memberId") memberId: Long,
        @Path("noteId") noteId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): ApiResponse<QuizListResponse>

    // 퀴즈 복습 제출 api
    @POST("/reviews/quiz/{noteId}")
    suspend fun submitQuizReview(
        @Path("noteId") noteId: String,
        @Body request: QuizReviewRequest
    ): ApiResponse<ReviewResponse>
}