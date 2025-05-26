package com.example.tokkit

import android.util.Log
import androidx.lifecycle.*
import com.example.tokkit.data.remote.model.Note
import com.example.tokkit.data.remote.repository.NoteRepository
import com.example.tokkit.data.remote.repository.ReviewRepository
import kotlinx.coroutines.launch

class ReviewViewModel : ViewModel() {

    private val noteRepo = NoteRepository()
    private val reviewRepo = ReviewRepository()

    private val _notes = MutableLiveData<List<Note>>()
    val notes: LiveData<List<Note>> get() = _notes

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun loadNotesWithStages(memberId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val notesResult = noteRepo.getNotes(memberId, page = 0, size = 50)
                val stageMap = reviewRepo.getReviewStages()

                val withStage = notesResult.notes.map { note ->
                    val stageStr = stageMap[note.id]
                    val stageTag = stageStr
                        ?.filter { it.isDigit() }
                        ?.toIntOrNull()
//                        ?.plus(1)
                        ?.let { "단계$it" }

                    if (note.previewContent.isNullOrBlank()) {
                        Log.w("ReviewViewModel", "previewContent 비어있음: ${note.title} (id=${note.id})")
                    }

                    val updatedTags = (note.tags ?: emptyList()) +
                            (stageTag?.let { listOf(it) } ?: emptyList())

                    note.copy(tags = updatedTags)
                }

                _notes.value = withStage

            } catch (e: Exception) {
                Log.e("ReviewViewModel", "노트 로딩 실패", e)
                _error.value = e.message
                _notes.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
