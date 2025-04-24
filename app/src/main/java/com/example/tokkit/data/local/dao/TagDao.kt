package com.example.tokkit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tokkit.data.local.entities.Tag

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: Tag)

    @Query("SELECT * FROM tag WHERE id = :id")
    suspend fun getTagById(id: Long): Tag?

    @Query("SELECT * FROM tag")
    suspend fun getAllTags(): List<Tag>

    @Delete
    suspend fun delete(tag: Tag)

    //@Query("SELECT * FROM tag WHERE name LIKE :query || '%' ORDER BY name ASC")
    @Query("SELECT * FROM tag WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun getTagsByQuery(query: String): List<Tag>

}
