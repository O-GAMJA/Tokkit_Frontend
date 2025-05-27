package com.example.tokkit.util

import com.example.tokkit.data.remote.api.AuthApi
import com.example.tokkit.data.remote.api.ImageApiService
import com.example.tokkit.data.remote.api.NoteApiService
import com.example.tokkit.data.remote.api.S3ApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

     private const val BASE_URL = "http://52.79.86.14:8080"
    // private const val BASE_URL = "http://10.0.2.2:8080" // -> 로컬테스트용

    // OkHttpClient 인스턴스 생성 - 타임아웃 설정 추가
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)  // 연결 타임아웃 30초
        .readTimeout(60, TimeUnit.SECONDS)     // 읽기 타임아웃 60초
        .writeTimeout(60, TimeUnit.SECONDS)    // 쓰기 타임아웃 60초
        .retryOnConnectionFailure(true)        // 연결 실패시 재시도
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)  // 수정된 클라이언트 적용
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val noteApi: NoteApiService by lazy {
        retrofit.create(NoteApiService::class.java)
    }

    val imageApi: ImageApiService by lazy {
        retrofit.create(ImageApiService::class.java)
    }

    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }

    val s3Api: S3ApiService by lazy {
        retrofit.create(S3ApiService::class.java)
    }

    // ✅ 로그인 관련 API 추가
    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }
}