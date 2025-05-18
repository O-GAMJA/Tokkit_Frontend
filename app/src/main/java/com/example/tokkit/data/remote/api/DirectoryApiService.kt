package com.example.tokkit.data.remote.api

import com.example.tokkit.data.remote.model.ApiResponse
import com.example.tokkit.data.remote.model.Directory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface DirectoryApiService {
    @GET("/notes/directory/tree")
    suspend fun getDirectoryTree(
        @Query("memberId") memberId: Long
    ): ApiResponse<List<Directory>>

    @POST("/notes/directory")
    suspend fun createDirectory(
        @Query("name") name: String,
        @Query("parentDirectoryId") parentDirectoryId: Int?
    ): ApiResponse<Directory>
}