package com.example.tokkit.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmark",
    foreignKeys = [
        ForeignKey(
            entity = NoteMetadata::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val note_id: String,
    val bookmark_folder: String?,
    val created_at: String
)
