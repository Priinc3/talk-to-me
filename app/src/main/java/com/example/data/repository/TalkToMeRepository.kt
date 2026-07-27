package com.example.data.repository

import com.example.data.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class TalkToMeRepository(private val database: AppDatabase) {

    val todos: Flow<List<TodoEntity>> = database.todoDao().getAllTodos()
    val calendarEvents: Flow<List<CalendarBlockEntity>> = database.calendarDao().getAllEvents()
    val nextEvent: Flow<CalendarBlockEntity?> = database.calendarDao().getNextEvent()
    val reminders: Flow<List<ReminderEntity>> = database.reminderDao().getAllReminders()
    val nextReminder: Flow<ReminderEntity?> = database.reminderDao().getNextReminder()
    val alarms: Flow<List<VoiceAlarmEntity>> = database.voiceAlarmDao().getAllAlarms()
    val nextAlarm: Flow<VoiceAlarmEntity?> = database.voiceAlarmDao().getNextAlarm()
    val meetingNotes: Flow<List<MeetingNoteEntity>> = database.meetingNoteDao().getAllNotes()
    val latestAction: Flow<ActionHistoryEntity?> = database.actionHistoryDao().getLatestAction()

    suspend fun insertTodo(todo: TodoEntity): Long = database.todoDao().insert(todo)
    suspend fun updateTodo(todo: TodoEntity) = database.todoDao().update(todo)
    suspend fun deleteTodo(id: Int) = database.todoDao().deleteById(id)

    suspend fun insertCalendarBlock(event: CalendarBlockEntity): Long = database.calendarDao().insert(event)
    suspend fun deleteCalendarBlock(id: Int) = database.calendarDao().deleteById(id)

    suspend fun insertReminder(reminder: ReminderEntity): Long = database.reminderDao().insert(reminder)
    suspend fun updateReminder(reminder: ReminderEntity) = database.reminderDao().update(reminder)
    suspend fun deleteReminder(id: Int) = database.reminderDao().deleteById(id)

    suspend fun insertVoiceAlarm(alarm: VoiceAlarmEntity): Long = database.voiceAlarmDao().insert(alarm)
    suspend fun updateVoiceAlarm(alarm: VoiceAlarmEntity) = database.voiceAlarmDao().update(alarm)
    suspend fun deleteVoiceAlarm(id: Int) = database.voiceAlarmDao().deleteById(id)

    suspend fun insertMeetingNote(note: MeetingNoteEntity): Long = database.meetingNoteDao().insert(note)
    suspend fun deleteMeetingNote(id: Int) = database.meetingNoteDao().deleteById(id)

    suspend fun recordActionHistory(
        transcript: String,
        summaryText: String,
        createdDetails: String,
        todoIds: List<Long> = emptyList(),
        calendarIds: List<Long> = emptyList(),
        reminderIds: List<Long> = emptyList(),
        alarmIds: List<Long> = emptyList(),
        noteIds: List<Long> = emptyList()
    ): Long {
        val entity = ActionHistoryEntity(
            transcript = transcript,
            summaryText = summaryText,
            createdDetails = createdDetails,
            todoIds = todoIds.joinToString(","),
            calendarIds = calendarIds.joinToString(","),
            reminderIds = reminderIds.joinToString(","),
            alarmIds = alarmIds.joinToString(","),
            noteIds = noteIds.joinToString(",")
        )
        return database.actionHistoryDao().insert(entity)
    }

    suspend fun undoLastAction(action: ActionHistoryEntity) {
        action.todoIds.split(",").filter { it.isNotBlank() }.forEach { idStr ->
            idStr.toIntOrNull()?.let { database.todoDao().deleteById(it) }
        }
        action.calendarIds.split(",").filter { it.isNotBlank() }.forEach { idStr ->
            idStr.toIntOrNull()?.let { database.calendarDao().deleteById(it) }
        }
        action.reminderIds.split(",").filter { it.isNotBlank() }.forEach { idStr ->
            idStr.toIntOrNull()?.let { database.reminderDao().deleteById(it) }
        }
        action.alarmIds.split(",").filter { it.isNotBlank() }.forEach { idStr ->
            idStr.toIntOrNull()?.let { database.voiceAlarmDao().deleteById(it) }
        }
        action.noteIds.split(",").filter { it.isNotBlank() }.forEach { idStr ->
            idStr.toIntOrNull()?.let { database.meetingNoteDao().deleteById(it) }
        }
        database.actionHistoryDao().deleteById(action.id)
    }
}
