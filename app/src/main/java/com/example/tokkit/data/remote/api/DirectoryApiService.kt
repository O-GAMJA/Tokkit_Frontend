package com.example.tokkit.data.remote.api

import com.example.tokkit.data.remote.model.ApiResponse
import com.example.tokkit.data.remote.model.Directory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Body

interface DirectoryApiService {
    @GET("/notes/directory/tree")
    suspend fun getDirectoryTree(
        @Query("memberId") memberId: Long
    ): ApiResponse<List<Directory>>

    @POST("/directories")
    suspend fun createDirectory(
        @Query("member_id") memberId: Long,
        @Body requestBody: Map<String, String>
    ): ApiResponse<Any>
}