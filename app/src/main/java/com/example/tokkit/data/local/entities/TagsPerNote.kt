package com.example.tokkit.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "tagsPerNote",
    primaryKeys = ["tag_id", "note_id"], // 복합 기본키
    foreignKeys = [
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = NoteMetadata::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TagsPerNote(
    val tag_id: String,
    val note_id: String
)
