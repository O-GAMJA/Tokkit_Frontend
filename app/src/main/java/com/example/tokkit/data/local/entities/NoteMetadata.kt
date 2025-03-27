package com.example.tokkit.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "noteMetadata")
data class NoteMetadata(
    @PrimaryKey val id: String, // 노트 기본키 (uuid 이용 예정)
    val directory_name: String, // 디렉토리 이름
    val title: String,          // 노트 제목
    val content: String,        // 노트 내용
    val created_at: String,     // Room은 복잡한 타입을 직접 저장 X (TypeConverter)
    val updated_at: String
)
