package com.example.tokkit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tokkit.data.local.entities.OtherUserNote

@Dao
interface OtherUserNoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(otherNote: OtherUserNote)

    @Query("SELECT * FROM otherUserNote WHERE note_id = :noteId")
    suspend fun getByNoteId(noteId: String): OtherUserNote?

    @Delete
    suspend fun delete(otherNote: OtherUserNote)
}
