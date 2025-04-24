package com.example.tokkit.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.tokkit.data.local.dao.*
import com.example.tokkit.data.local.entities.*

@Database(
    entities = [
        NoteMetadata::class,
        MyNote::class,
        OtherUserNote::class,
        Bookmark::class,
        Tag::class,
        TagsPerNote::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteMetadataDao(): NoteMetadataDao
    abstract fun myNoteDao(): MyNoteDao
    abstract fun otherUserNoteDao(): OtherUserNoteDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun tagDao(): TagDao
    abstract fun tagsPerNoteDao(): TagsPerNoteDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tokkit_database"
                ).build().also { instance = it }
            }
        }
    }
}
