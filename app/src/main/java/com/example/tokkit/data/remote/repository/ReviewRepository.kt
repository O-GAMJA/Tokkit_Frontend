package com.example.tokkit.data.remote.repository

import android.util.Log
import com.example.tokkit.data.remote.api.ReviewApiService
import com.example.tokkit.data.remote.model.ConversationReviewRequest
import com.example.tokkit.data.remote.model.QuizItem
import com.example.tokkit.data.remote.model.QuizReviewRequest
import com.example.tokkit.data.remote.model.ReviewDetailResponse
import com.example.tokkit.data.remote.model.ReviewResponse
import com.example.tokkit.util.RetrofitClient
import com.google.android.material.color.utilities.Score

class ReviewRepository {
    private val api = RetrofitClient.createService(ReviewApiService::class.java)

    suspend fun getReviewStages(): Map<String, String> {
        return try {
            val response = api.getReviewStages()
            if (response.isSuccess) {
                response.result.associate { it.uuid to it.stage }
            } else {
                Log.e("ReviewRepository", "API 실패: ${response.message}")
                emptyMap()
            }
        } catch (e: Exception) {
            Log.e("ReviewRepository", "예외 발생", e)
            emptyMap()
        }
    }

    suspend fun getReviewDetail(noteId: String): ReviewDetailResponse? {
        return try {
            val response = RetrofitClient.createService(ReviewApiService::class.java).getReviewDetail(noteId)
            if (response.isSuccess) response.result else null
        } catch (e: Exception) {
            Log.e("ReviewRepository", "복습 상세 조회 실패", e)
            null
        }
    }

    suspend fun submitConversationReview(noteId: String, content: String): ReviewResponse? {
        return try {
            val response = api.submitConversationReview(noteId, ConversationReviewRequest(content))
            if (response.isSuccess) {
                response.result
            } else {
                Log.e("ReviewRepository", "복습 제출 실패: ${response.message}")
                null
            }
        } catch (e: Exception) {
            Log.e("ReviewRepository", "복습 제출 예외", e)
            null
        }
    }

    private val quizApi = RetrofitClient.createService(ReviewApiService::class.java)

    suspend fun getQuizzes(memberId: Long, noteId: String): List<QuizItem> {
        return try {
            val response = quizApi.getQuizzes(memberId, noteId)
            if (response.isSuccess) response.result.quizzes else emptyList()
        } catch (e: Exception) {
            Log.e("ReviewRepository", "퀴즈 불러오기 실패", e)
            emptyList()
        }
    }

    suspend fun submitQuizReview(noteId: String, score: Int, correctCount: Int): ReviewResponse? {
        return try {
            val response = api.submitQuizReview(noteId, QuizReviewRequest(score, correctCount))
            if (response.isSuccess) {
                response.result
            } else {
                Log.e("ReviewRepository", "복습 제출 실패: ${response.message}")
                null
            }
        } catch (e: Exception) {
            Log.e("ReviewRepository", "복습 제출 예외", e)
            null
        }
    }

}