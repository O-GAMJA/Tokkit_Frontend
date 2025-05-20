package com.example.tokkit

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tokkit.data.remote.model.CommentResponse
import com.example.tokkit.data.remote.model.Note
import com.example.tokkit.data.remote.model.NoteListResult
import com.example.tokkit.data.remote.model.SimilarNoteItem
import com.example.tokkit.data.remote.repository.NoteRepository
import kotlinx.coroutines.launch

class NoteViewModel : ViewModel() {
    private val repository = NoteRepository()

    internal val _notes = MutableLiveData<List<Note>>()
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

    // 태그 검색 모드 여부를 나타내는 변수
    private val _isTagSearchMode = MutableLiveData<Boolean>(false)
    val isTagSearchMode: LiveData<Boolean> get() = _isTagSearchMode

    // 현재 검색 중인 태그 이름
    private var currentTagName: String? = null
    private var tagSearchPage = 0

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
        exitTagSearchMode()  // 태그 검색 모드 종료
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

    fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }

    fun clearNotes() {
        _notes.value = emptyList()
    }

    fun updateNotesFromTagSearch(noteListResult: NoteListResult) {
        // 먼저 기존 목록 초기화
        resetNotes()

        // 새 목록으로 업데이트
        _notes.value = noteListResult.notes
        _isLastPage.value = noteListResult.paginationInfo.isLast
        currentPage = noteListResult.paginationInfo.page

        Log.d("NoteViewModel", "태그 검색 결과 업데이트: ${noteListResult.notes.size}개")
    }

    // 태그로 검색 시 호출하는 함수
    fun searchNotesByTag(tagName: String, memberId: Long, page: Int = 0, size: Int = 10) {
        _isLoading.value = true
        currentTagName = tagName
        tagSearchPage = page
        _isTagSearchMode.value = true

        viewModelScope.launch {
            try {
                val result = repository.getNotesByTag(tagName, memberId, page, size)
                // 태그 검색 결과로 노트 목록 초기화
                _notes.value = result.notes
                _isLastPage.value = result.paginationInfo.isLast
            } catch (e: Exception) {
                _error.value = e.message ?: "태그 검색 실패"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 태그 검색 모드에서 더 많은 노트 로드
    fun loadMoreNotesByTag() {
        if (isLoadingPage || _isLastPage.value == true || currentTagName == null) return

        isLoadingPage = true
        tagSearchPage++

        viewModelScope.launch {
            try {
                val result = repository.getNotesByTag(currentTagName!!, 1L, tagSearchPage, pageSize)
                val currentList = _notes.value ?: emptyList()
                _notes.value = currentList + result.notes
                _isLastPage.value = result.paginationInfo.isLast
            } catch (e: Exception) {
                Log.e("NoteViewModel", "태그 검색 추가 페이지 로드 실패", e)
            } finally {
                isLoadingPage = false
            }
        }
    }

    // 태그 검색 모드 종료
    fun exitTagSearchMode() {
        _isTagSearchMode.value = false
        currentTagName = null
        tagSearchPage = 0
    }
}