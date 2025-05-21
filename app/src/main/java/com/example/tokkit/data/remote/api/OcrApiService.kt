package com.example.tokkit.data.remote.api

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import com.example.tokkit.data.remote.model.ApiResponse
import com.example.tokkit.data.remote.model.OcrResponse

interface OcrApiService {
    @Multipart
    @POST("/ocr")
    suspend fun processImage(
        @Part image: MultipartBody.Part
    ): ApiResponse<OcrResponse>
}