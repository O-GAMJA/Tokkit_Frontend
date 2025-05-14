package com.example.tokkit

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tokkit.data.remote.model.Note
import com.example.tokkit.data.remote.repository.NoteRepository
import kotlinx.coroutines.launch

class NoteViewModel : ViewModel() {
    private val repository = NoteRepository()

    private val _notes = MutableLiveData<List<Note>>()
    val notes: LiveData<List<Note>> get() = _notes

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun loadNotes(memberId: Long) {
        _isLoading.value = true
        Log.d("NoteViewModel", "loadNotes 시작 - memberId: $memberId")

        viewModelScope.launch {
            try {
                val result = repository.getNotes(memberId)
                Log.d("NoteViewModel", "노트 수: ${result.size}")
                _notes.value = result
            } catch (e: Exception) {
                Log.e("NoteViewModel", "예외 발생: ${e.message}", e)
                _notes.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private val _selectedNote = MutableLiveData<Note?>()
    val selectedNote: LiveData<Note?> get() = _selectedNote

    fun loadNoteById(noteId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _selectedNote.value = repository.getNoteById(noteId)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleEmoji(noteId: String, emojiType: String, isClicked: Boolean) {
        viewModelScope.launch {
            val response = repository.toggleEmoji(noteId, emojiType, isClicked)
            val existingNote = _selectedNote.value
            if (response != null && existingNote != null) {
                val updatedNote = existingNote.copy(
                    emojiStatus = response.emojiStatus
                )
                _selectedNote.value = updatedNote
            }
        }
    }




}