package com.example.tokkit.data.remote.api

import retrofit2.http.GET
import com.example.tokkit.data.remote.model.ApiResponse
import com.example.tokkit.data.remote.model.ReviewDetailResponse
import com.example.tokkit.data.remote.model.ReviewStageItem
import retrofit2.http.Path

interface ReviewApiService {

    // 모든 노트의 복습 단계 조회
    @GET("/reviews/all")
    suspend fun getReviewStages(): ApiResponse<List<ReviewStageItem>>

    // 복습 상세 통계 조회
    @GET("/reviews/{noteId}")
    suspend fun getReviewDetail(
        @Path("noteId") noteId: String
    ): ApiResponse<ReviewDetailResponse>
}