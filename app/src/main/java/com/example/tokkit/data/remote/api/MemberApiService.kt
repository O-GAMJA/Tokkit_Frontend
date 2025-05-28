package com.example.tokkit.data.remote.api

import com.example.tokkit.data.remote.model.ApiResponse
import com.example.tokkit.data.remote.model.MemberProfileResponse
import retrofit2.http.GET

interface MemberApiService {
    @GET("/members/")
    suspend fun getProfile(): ApiResponse<MemberProfileResponse>
}
