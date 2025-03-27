package com.example.tokkit.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "otherUserNote",
    foreignKeys = [
        ForeignKey(
            entity = NoteMetadata::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class OtherUserNote(
    @PrimaryKey val note_id: String, // NoteMetadata의 id 참조
    val note_author_id: String       // 외부 유저 ID
)
