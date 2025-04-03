package com.example.tokkit

import android.app.Application
import androidx.lifecycle.*
import com.example.tokkit.data.local.database.AppDatabase
import com.example.tokkit.data.local.entities.Tag
import kotlinx.coroutines.launch

class TagViewModel(application: Application) : AndroidViewModel(application) {

    private val tagDao = AppDatabase.getDatabase(application).tagDao()

    private val _filteredTags = MutableLiveData<List<Tag>>()
    val filteredTags: LiveData<List<Tag>> get() = _filteredTags

    fun searchTags(query: String) {
        viewModelScope.launch {
            _filteredTags.value = tagDao.getTagsByQuery(query)
        }
    }
}
