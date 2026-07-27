package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY createdTimestamp DESC")
    fun getAllTodos(): Flow<List<TodoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: TodoEntity): Long

    @Update
    suspend fun update(todo: TodoEntity)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface CalendarDao {
    @Query("SELECT * FROM calendar_blocks ORDER BY startTimeMillis ASC")
    fun getAllEvents(): Flow<List<CalendarBlockEntity>>

    @Query("SELECT * FROM calendar_blocks WHERE startTimeMillis >= :fromTime ORDER BY startTimeMillis ASC LIMIT 1")
    fun getNextEvent(fromTime: Long = System.currentTimeMillis()): Flow<CalendarBlockEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: CalendarBlockEntity): Long

    @Query("DELETE FROM calendar_blocks WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY remindAtMillis ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE remindAtMillis >= :fromTime AND isTriggered = 0 ORDER BY remindAtMillis ASC LIMIT 1")
    fun getNextReminder(fromTime: Long = System.currentTimeMillis()): Flow<ReminderEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface VoiceAlarmDao {
    @Query("SELECT * FROM voice_alarms ORDER BY triggerTimeMillis ASC")
    fun getAllAlarms(): Flow<List<VoiceAlarmEntity>>

    @Query("SELECT * FROM voice_alarms WHERE triggerTimeMillis >= :fromTime AND isEnabled = 1 ORDER BY triggerTimeMillis ASC LIMIT 1")
    fun getNextAlarm(fromTime: Long = System.currentTimeMillis()): Flow<VoiceAlarmEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: VoiceAlarmEntity): Long

    @Update
    suspend fun update(alarm: VoiceAlarmEntity)

    @Query("DELETE FROM voice_alarms WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface MeetingNoteDao {
    @Query("SELECT * FROM meeting_notes ORDER BY createdAtMillis DESC")
    fun getAllNotes(): Flow<List<MeetingNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: MeetingNoteEntity): Long

    @Query("DELETE FROM meeting_notes WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface ActionHistoryDao {
    @Query("SELECT * FROM action_history ORDER BY timestamp DESC LIMIT 1")
    fun getLatestAction(): Flow<ActionHistoryEntity?>

    @Query("SELECT * FROM action_history ORDER BY timestamp DESC")
    fun getAllActions(): Flow<List<ActionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: ActionHistoryEntity): Long

    @Query("DELETE FROM action_history WHERE id = :id")
    suspend fun deleteById(id: Int)
}
