package com.example.tokkit

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tokkit.data.remote.model.CommentResponse
import com.example.tokkit.data.remote.model.Note
import com.example.tokkit.data.remote.model.SimilarNoteItem
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

    private val _isLastPage = MutableLiveData<Boolean>()
    val isLastPage: LiveData<Boolean> get() = _isLastPage

    private var currentPage = 0
    private val pageSize = 10
    private var isLoadingPage = false

    fun loadNotes(memberId: Long, page: Int= 0, size: Int = 10) {
        _isLoading.value = true
        currentPage = page
        Log.d("NoteViewModel", "loadNotes 시작 - memberId: $memberId, page: $page, size: $size")

        viewModelScope.launch {
            try {
                val result = repository.getNotes(memberId = memberId, page, size)
                Log.d("NoteViewModel", "노트 수: ${result.notes.size}")
                _notes.value = result.notes
                _isLastPage.value = result.paginationInfo.isLast
            } catch (e: Exception) {
                Log.e("NoteViewModel", "예외 발생: ${e.message}", e)
                _notes.value = emptyList()
                _error.value = e.message ?: "노트 불러오기 실패"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetNotes() {
        currentPage = 0
        _notes.value = emptyList()
        _isLastPage.value = false
    }

    fun loadMoreNotes(memberId: Long) {
        if (isLoadingPage || _isLastPage.value == true) return

        isLoadingPage = true
        viewModelScope.launch {
            try {
                val newResult = repository.getNotes(memberId, currentPage, pageSize)
                val currentList = _notes.value ?: emptyList()
                _notes.value = currentList + newResult.notes
                _isLastPage.value = newResult.paginationInfo.isLast
                currentPage++
            } catch (e: Exception) {
                Log.e("NoteViewModel", "페이지 로드 실패", e)
            } finally {
                isLoadingPage = false
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

    fun toggleBookmark(noteId: String, isBookmarked: Boolean) {
        viewModelScope.launch {
            val status = repository.toggleBookmark(noteId, isBookmarked)
            val current = _selectedNote.value
            if (status != null && current != null) {
                _selectedNote.value = current.copy(bookmarkStatus = status)
            }
        }
    }

    val comments = MutableLiveData<List<CommentResponse>>()
    private val _isLastCommentPage = MutableLiveData<Boolean>()
    val isLastCommentPage: LiveData<Boolean> get() = _isLastCommentPage
    private val _totalCommentCount = MutableLiveData<Int>()
    val totalCommentCount: LiveData<Int> get() = _totalCommentCount
    private var currentCommentPage = 0

    fun loadComments(noteId: String, page: Int = 0, size: Int = 10) {
        viewModelScope.launch {
            val result = repository.fetchComments(noteId, page, size)
            result?.let {
                comments.value = it.comments
                _totalCommentCount.value = it.totalElements
                _isLastCommentPage.value = it.last
                currentCommentPage = page
            }
        }
    }

    fun resetComments() {
        comments.value = emptyList()
        _isLastCommentPage.value = false
        currentCommentPage = 0
    }

    fun loadNextCommentPage(noteId: String, size: Int = 10) {
        if (_isLastCommentPage.value == true) return
        loadComments(noteId, currentCommentPage + 1, size)
    }

    fun postComment(
        noteId: String,
        content: String,
        parentId: Long? = null,
        onSuccess: () -> Unit,
        onFail: () -> Unit
    ) {
        viewModelScope.launch {
            val success = repository.writeComment(noteId, content, parentId)
            if (success) {
                resetComments()                     // 기존 댓글 초기화
                loadComments(noteId, page = 0)      // 첫 페이지 강제 로드
                onSuccess()
            } else {
                onFail()
            }
        }
    }

    private val _similarNotes = MutableLiveData<List<SimilarNoteItem>>()
    val similarNotes: LiveData<List<SimilarNoteItem>> get() = _similarNotes

    fun loadSimilarNotes(noteId: String) {
        viewModelScope.launch {
            _similarNotes.value = repository.fetchSimilarNotes(noteId)
        }
    }


}