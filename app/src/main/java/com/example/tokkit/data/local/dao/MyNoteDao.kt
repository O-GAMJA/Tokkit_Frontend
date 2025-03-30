package com.example.tokkit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tokkit.data.local.entities.MyNote

@Dao
interface MyNoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(myNote: MyNote)

    @Query("SELECT * FROM myNote WHERE note_id = :noteId")
    suspend fun getByNoteId(noteId: String): MyNote?

    @Delete
    suspend fun delete(myNote: MyNote)
}
