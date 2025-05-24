package com.example.tokkit.data.remote.repository

import android.util.Log
import com.example.tokkit.data.remote.api.ReviewApiService
import com.example.tokkit.data.remote.model.ReviewStageItem
import com.example.tokkit.util.RetrofitClient

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
}