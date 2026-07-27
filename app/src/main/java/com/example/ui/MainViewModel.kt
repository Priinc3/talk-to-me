package com.example.ui

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ZenAIFunctionCaller
import com.example.data.AppDatabase
import com.example.data.model.*
import com.example.data.repository.TalkToMeRepository
import com.example.receiver.ReminderNotificationReceiver
import com.example.speech.SpeechToTextManager
import com.example.speech.TextToSpeechManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TalkToMeRepository
    private val sttManager: SpeechToTextManager
    private val ttsManager: TextToSpeechManager
    private val intentParser = ZenAIFunctionCaller()

    val todos: StateFlow<List<TodoEntity>>
    val calendarEvents: StateFlow<List<CalendarBlockEntity>>
    val nextEvent: StateFlow<CalendarBlockEntity?>
    val reminders: StateFlow<List<ReminderEntity>>
    val nextReminder: StateFlow<ReminderEntity?>
    val alarms: StateFlow<List<VoiceAlarmEntity>>
    val nextAlarm: StateFlow<VoiceAlarmEntity?>
    val meetingNotes: StateFlow<List<MeetingNoteEntity>>
    val latestAction: StateFlow<ActionHistoryEntity?>

    val isListening: StateFlow<Boolean>
    val currentTranscript: StateFlow<String>
    val sttError: StateFlow<String?>

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _userFeedback = MutableStateFlow<String?>(null)
    val userFeedback: StateFlow<String?> = _userFeedback

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TalkToMeRepository(database)
        sttManager = SpeechToTextManager(application)
        ttsManager = TextToSpeechManager(application)

        todos = repository.todos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        calendarEvents = repository.calendarEvents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        nextEvent = repository.nextEvent.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        reminders = repository.reminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        nextReminder = repository.nextReminder.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        alarms = repository.alarms.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        nextAlarm = repository.nextAlarm.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        meetingNotes = repository.meetingNotes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        latestAction = repository.latestAction.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        isListening = sttManager.isListening
        currentTranscript = sttManager.transcript
        sttError = sttManager.error
    }

    fun startListening() {
        sttManager.startListening { transcript ->
            processVoiceCommand(transcript)
        }
    }

    fun stopListening() {
        sttManager.stopListening()
    }

    fun processVoiceCommand(transcript: String) {
        if (transcript.isBlank()) return
        viewModelScope.launch {
            _isProcessing.value = true
            _userFeedback.value = "Analyzing voice intent..."
            try {
                val parsed = intentParser.parseVoiceCommand(transcript)

                val todoIds = mutableListOf<Long>()
                val calendarIds = mutableListOf<Long>()
                val reminderIds = mutableListOf<Long>()
                val alarmIds = mutableListOf<Long>()
                val noteIds = mutableListOf<Long>()

                parsed.todosToCreate.forEach { todoIds.add(repository.insertTodo(it)) }
                parsed.calendarBlocksToCreate.forEach { calendarIds.add(repository.insertCalendarBlock(it)) }
                parsed.remindersToCreate.forEach { reminder ->
                    val id = repository.insertReminder(reminder)
                    reminderIds.add(id)
                    scheduleNotification(reminder.message, reminder.remindAtMillis, id.toInt())
                }
                parsed.alarmsToCreate.forEach { alarm ->
                    val id = repository.insertVoiceAlarm(alarm)
                    alarmIds.add(id)
                    scheduleNotification("Voice Alarm: ${alarm.spokenMessage}", alarm.triggerTimeMillis, id.toInt())
                }
                parsed.meetingNotesToCreate.forEach { noteIds.add(repository.insertMeetingNote(it)) }

                val details = buildString {
                    if (parsed.calendarBlocksToCreate.isNotEmpty()) append("• Added to Calendar (${parsed.calendarBlocksToCreate.size})\n")
                    if (parsed.remindersToCreate.isNotEmpty()) append("• Reminder set\n")
                    if (parsed.todosToCreate.isNotEmpty()) append("• Added to To-Do List (${parsed.todosToCreate.size})\n")
                    if (parsed.alarmsToCreate.isNotEmpty()) append("• Voice alarm set\n")
                    if (parsed.meetingNotesToCreate.isNotEmpty()) append("• Meeting note recorded\n")
                }.trim()

                repository.recordActionHistory(
                    transcript = transcript,
                    summaryText = parsed.summaryText,
                    createdDetails = if (details.isBlank()) "• Action recorded" else details,
                    todoIds = todoIds,
                    calendarIds = calendarIds,
                    reminderIds = reminderIds,
                    alarmIds = alarmIds,
                    noteIds = noteIds
                )

                _userFeedback.value = parsed.summaryText
                ttsManager.speak(parsed.spokenConfirmation)
            } catch (e: Exception) {
                _userFeedback.value = "Error parsing voice command: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun undoLastAction() {
        val last = latestAction.value ?: return
        viewModelScope.launch {
            repository.undoLastAction(last)
            _userFeedback.value = "Last voice action undone."
            ttsManager.speak("Undo complete.")
        }
    }

    fun toggleTodo(todo: TodoEntity) {
        viewModelScope.launch {
            repository.updateTodo(todo.copy(isDone = !todo.isDone))
        }
    }

    fun deleteTodo(id: Int) {
        viewModelScope.launch { repository.deleteTodo(id) }
    }

    fun addTodoManual(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.insertTodo(TodoEntity(text = text.trim()))
        }
    }

    fun deleteCalendarBlock(id: Int) {
        viewModelScope.launch { repository.deleteCalendarBlock(id) }
    }

    fun deleteReminder(id: Int) {
        viewModelScope.launch { repository.deleteReminder(id) }
    }

    fun deleteAlarm(id: Int) {
        viewModelScope.launch { repository.deleteVoiceAlarm(id) }
    }

    fun deleteMeetingNote(id: Int) {
        viewModelScope.launch { repository.deleteMeetingNote(id) }
    }

    fun testTts(text: String) {
        ttsManager.speak(text)
    }

    private fun scheduleNotification(message: String, triggerAtMillis: Long, requestId: Int) {
        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderNotificationReceiver::class.java).apply {
            putExtra("EXTRA_TITLE", "Talk to Me")
            putExtra("EXTRA_MESSAGE", message)
            putExtra("EXTRA_REMINDER_ID", requestId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        sttManager.stopListening()
        ttsManager.shutdown()
    }
}
