package com.example.tokkit.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "myNote",
    foreignKeys = [
        ForeignKey(
            entity = NoteMetadata::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MyNote(
    @PrimaryKey val note_id: String, // NoteMetadata의 id를 참조
    val is_public: Boolean,          // 노트 공개 여부
    val study_session_log: String?   // 공부 기록
)
