package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val dueDate: String? = null,
    val isDone: Boolean = false,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val sourceTranscript: String? = null
)

@Entity(tableName = "calendar_blocks")
data class CalendarBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val startTimeMillis: Long,
    val durationMinutes: Int = 30,
    val location: String? = null,
    val sourceTranscript: String? = null
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val message: String,
    val remindAtMillis: Long,
    val isTriggered: Boolean = false,
    val sourceTranscript: String? = null
)

@Entity(tableName = "voice_alarms")
data class VoiceAlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val triggerTimeMillis: Long,
    val spokenMessage: String,
    val isEnabled: Boolean = true,
    val sourceTranscript: String? = null
)

@Entity(tableName = "meeting_notes")
data class MeetingNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val transcript: String,
    val summary: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "action_history")
data class ActionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val transcript: String,
    val summaryText: String,
    val createdDetails: String, // Bullet points of actions taken
    val timestamp: Long = System.currentTimeMillis(),
    val todoIds: String = "", // Comma-separated IDs for Undo
    val calendarIds: String = "",
    val reminderIds: String = "",
    val alarmIds: String = "",
    val noteIds: String = ""
)
