package com.example.tokkit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tokkit.data.local.entities.TagsPerNote

@Dao
interface TagsPerNoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tagMap: TagsPerNote)

    @Query("SELECT * FROM tagsPerNote WHERE note_id = :noteId")
    suspend fun getTagsForNote(noteId: String): List<TagsPerNote>

    @Query("SELECT * FROM tagsPerNote WHERE tag_id = :tagId")
    suspend fun getNotesForTag(tagId: Long): List<TagsPerNote>

    @Delete
    suspend fun delete(tagMap: TagsPerNote)
}
