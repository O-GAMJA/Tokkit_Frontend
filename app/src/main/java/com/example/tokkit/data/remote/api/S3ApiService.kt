// util/RetrofitClient.kt에 S3ApiService 추가
package com.example.tokkit.data.remote.api

import com.example.tokkit.data.remote.model.ApiResponse
import com.example.tokkit.data.remote.model.S3UrlResponse
import retrofit2.http.POST
import retrofit2.http.Query

interface S3ApiService {
    @POST("/s3/preSignedUrl")
    suspend fun getPreSignedUrl(
        @Query("fileType") fileType: String,
        @Query("fileName") fileName: String
    ): ApiResponse<S3UrlResponse>
}

