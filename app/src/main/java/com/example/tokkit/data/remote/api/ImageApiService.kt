package com.example.tokkit.data.remote.api

import com.example.tokkit.data.remote.model.ImageGenerationRequest
import com.example.tokkit.data.remote.model.ImageGenerationResponse
import retrofit2.http.Body
import retrofit2.http.POST

// 이미지 생성 서비스
interface ImageApiService {
    @POST("/image-gen")
    suspend fun generateImage(@Body request: ImageGenerationRequest): ImageGenerationResponse
}