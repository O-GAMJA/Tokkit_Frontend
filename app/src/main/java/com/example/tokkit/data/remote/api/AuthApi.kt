package com.example.tokkit.data.remote.api

import com.example.tokkit.data.remote.model.LoginRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/auth/login")
    fun login(@Body request: LoginRequest): Call<Void> // 백엔드가 토큰 응답 주면 타입 변경
}