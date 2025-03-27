package com.example.tokkit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tokkit.data.local.entities.NoteMetadata

@Dao
interface NoteMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteMetadata)

    @Update
    suspend fun update(note: NoteMetadata)

    @Delete
    suspend fun delete(note: NoteMetadata)

    @Query("SELECT * FROM noteMetadata WHERE id = :id")
    suspend fun getNoteById(id: String): NoteMetadata?

    @Query("SELECT * FROM noteMetadata ORDER BY created_at DESC")
    suspend fun getAllNotes(): List<NoteMetadata>
}
