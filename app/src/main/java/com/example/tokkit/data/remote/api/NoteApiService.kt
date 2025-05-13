package com.example.tokkit.data.remote.api

import com.example.tokkit.data.remote.model.ApiResponse
import com.example.tokkit.data.remote.model.Note
import retrofit2.http.GET
import retrofit2.http.Path

interface NoteApiService {
    @GET("/notes/{memberId}/all")
    suspend fun getAllNotes(
        @Path("memberId") memberId: Long
    ): ApiResponse<List<Note>>
}
