package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.*
import com.example.data.model.*

@Database(
    entities = [
        TodoEntity::class,
        CalendarBlockEntity::class,
        ReminderEntity::class,
        VoiceAlarmEntity::class,
        MeetingNoteEntity::class,
        ActionHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun calendarDao(): CalendarDao
    abstract fun reminderDao(): ReminderDao
    abstract fun voiceAlarmDao(): VoiceAlarmDao
    abstract fun meetingNoteDao(): MeetingNoteDao
    abstract fun actionHistoryDao(): ActionHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "talk_to_me_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
