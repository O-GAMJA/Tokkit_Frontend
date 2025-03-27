package com.example.tokkit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tokkit.data.local.entities.Bookmark

@Dao
interface BookmarkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: Bookmark)

    @Query("SELECT * FROM bookmark WHERE note_id = :noteId")
    suspend fun getBookmarksByNoteId(noteId: String): List<Bookmark>

    @Delete
    suspend fun delete(bookmark: Bookmark)
}
