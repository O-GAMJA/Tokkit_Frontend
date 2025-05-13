package com.example.tokkit.data.remote.repository

import android.util.Log
import com.example.tokkit.data.remote.api.NoteApiService
import com.example.tokkit.data.remote.model.Note
import com.example.tokkit.util.RetrofitClient

class NoteRepository {
    private val api: NoteApiService = RetrofitClient.noteApi

    suspend fun getNotes(memberId: Long): List<Note> {
        return try {
            val response = api.getAllNotes(memberId)
            Log.d("NoteRepository", "서버 응답: $response")
            if (response.isSuccess && response.result != null) {
                Log.d("NoteRepository", "파싱 성공: ${response.result.size}")
                return response.result
            } else {
                Log.d("NoteRepository", "파싱 실패 또는 result가 null")
                return emptyList()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getNoteById(noteId: String): Note? {
        return try {
            val response = api.getNoteById(noteId)
            if (response.isSuccess) response.result else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}
