package com.example.tokkit.util

import com.example.tokkit.data.remote.api.NoteApiService
import com.example.tokkit.data.remote.model.ApiResponse
import com.example.tokkit.data.remote.model.Note
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "http://52.79.86.14:8080"

//    private val gson: Gson by lazy {
//        val type = object : TypeToken<ApiResponse<List<Note>>>() {}.type
//
//        GsonBuilder()
//            .registerTypeAdapter(type, ApiResponseDeserializer<List<Note>>())
//            .create()
//    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val noteApi: NoteApiService by lazy {
        retrofit.create(NoteApiService::class.java)
    }
}
