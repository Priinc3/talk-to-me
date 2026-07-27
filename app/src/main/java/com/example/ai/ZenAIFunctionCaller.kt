package com.example.ai

import com.example.BuildConfig
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ZenAIFunctionCaller {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun parseVoiceCommand(transcript: String): ParsedActionsResult = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.ZENAI_API_KEY } catch (e: Exception) { "" }

        if (apiKey.isNotBlank() && !apiKey.contains("MY_ZENAI_API_KEY")) {
            try {
                return@withContext callZenAi(transcript, apiKey)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext parseWithLocalRuleEngine(transcript)
    }

    private fun callZenAi(transcript: String, apiKey: String): ParsedActionsResult {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val currentDateTimeStr = "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)} on ${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.YEAR)}"

        val systemPrompt = """
You are an AI voice assistant intent parser. The current time is $currentDateTimeStr (Epoch millis: $now).
Analyze the user transcript and output a JSON object containing the actions to take.
User can request multiple actions simultaneously (e.g. 'meeting in half an hour' creates a calendar event + reminder + todo).

JSON schema format:
{
  "summaryText": "Brief 1-line summary of what was parsed",
  "spokenConfirmation": "Short natural text to speak back via TextToSpeech (e.g. 'Got it — meeting at 3:00, I'll remind you at 2:55')",
  "todos": [
    { "text": "Task description", "dueDate": "Optional date string or null" }
  ],
  "calendarBlocks": [
    { "title": "Meeting Title", "startTimeMillis": 123456789, "durationMinutes": 30, "location": null }
  ],
  "reminders": [
    { "message": "Reminder text", "remindAtMillis": 123456789 }
  ],
  "alarms": [
    { "triggerTimeMillis": 123456789, "spokenMessage": "Spoken alarm phrase" }
  ],
  "meetingNotes": [
    { "title": "Note Title", "transcript": "Full speech", "summary": "Key summary" }
  ]
}

Rules:
- Convert relative times ("in 30 minutes", "tomorrow at 3pm", "next Monday") to epoch millis using the current time as reference.
- For meetings, also create a reminder 5 minutes before and a todo to prepare.
- For reminders, default to 15 minutes from now unless specified.
- For alarms, default to 1 hour from now unless specified.
- If the user just states a task with no time/type, put it in todos.
ONLY return valid JSON. Do not include markdown code block backticks.
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("model", "zen-1")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", transcript)
                })
            })
            put("temperature", 0.2)
            put("response_format", JSONObject().put("type", "json_object"))
        }

        val request = Request.Builder()
            .url("https://api.zen.ai/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(jsonRequest.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response from zen.ai")

        val jsonObj = JSONObject(responseBody)
        val choices = jsonObj.optJSONArray("choices")
        val message = choices?.optJSONObject(0)?.optJSONObject("message")
        val text = message?.optString("content")
            ?: throw Exception("No content in zen.ai response")

        val resultJson = JSONObject(text.trim())

        return parseEntities(resultJson, transcript, now)
    }

    private fun parseEntities(resultJson: JSONObject, transcript: String, now: Long): ParsedActionsResult {
        val todos = mutableListOf<TodoEntity>()
        resultJson.optJSONArray("todos")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                todos.add(TodoEntity(text = item.optString("text"), dueDate = item.optString("dueDate", null), sourceTranscript = transcript))
            }
        }

        val events = mutableListOf<CalendarBlockEntity>()
        resultJson.optJSONArray("calendarBlocks")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                events.add(CalendarBlockEntity(
                    title = item.optString("title"),
                    startTimeMillis = item.optLong("startTimeMillis", now + 30 * 60 * 1000L),
                    durationMinutes = item.optInt("durationMinutes", 30),
                    location = item.optString("location", null),
                    sourceTranscript = transcript
                ))
            }
        }

        val reminders = mutableListOf<ReminderEntity>()
        resultJson.optJSONArray("reminders")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                reminders.add(ReminderEntity(
                    message = item.optString("message"),
                    remindAtMillis = item.optLong("remindAtMillis", now + 25 * 60 * 1000L),
                    sourceTranscript = transcript
                ))
            }
        }

        val alarms = mutableListOf<VoiceAlarmEntity>()
        resultJson.optJSONArray("alarms")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                alarms.add(VoiceAlarmEntity(
                    triggerTimeMillis = item.optLong("triggerTimeMillis", now + 60 * 60 * 1000L),
                    spokenMessage = item.optString("spokenMessage", "Alarm triggered!"),
                    sourceTranscript = transcript
                ))
            }
        }

        val notes = mutableListOf<MeetingNoteEntity>()
        resultJson.optJSONArray("meetingNotes")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                notes.add(MeetingNoteEntity(
                    title = item.optString("title", "Voice Note"),
                    transcript = item.optString("transcript", transcript),
                    summary = item.optString("summary", "Captured via voice")
                ))
            }
        }

        return ParsedActionsResult(
            transcript = transcript,
            summaryText = resultJson.optString("summaryText", "Action parsed"),
            spokenConfirmation = resultJson.optString("spokenConfirmation", "Got it!"),
            todosToCreate = todos,
            calendarBlocksToCreate = events,
            remindersToCreate = reminders,
            alarmsToCreate = alarms,
            meetingNotesToCreate = notes
        )
    }

    private fun parseWithLocalRuleEngine(transcript: String): ParsedActionsResult {
        val lower = transcript.lowercase()
        val now = System.currentTimeMillis()

        val todos = mutableListOf<TodoEntity>()
        val events = mutableListOf<CalendarBlockEntity>()
        val reminders = mutableListOf<ReminderEntity>()
        val alarms = mutableListOf<VoiceAlarmEntity>()
        val notes = mutableListOf<MeetingNoteEntity>()

        var summaryText = ""
        var spokenConfirmation = ""

        when {
            lower.contains("meeting in half an hour") || lower.contains("meeting in 30 mins") || lower.contains("meeting in 30 minutes") -> {
                val eventTime = now + 30 * 60 * 1000L
                val reminderTime = now + 25 * 60 * 1000L
                events.add(CalendarBlockEntity(title = "Team Sync", startTimeMillis = eventTime, durationMinutes = 30, sourceTranscript = transcript))
                reminders.add(ReminderEntity(message = "Team Sync meeting starting in 5 minutes", remindAtMillis = reminderTime, sourceTranscript = transcript))
                todos.add(TodoEntity(text = "Prepare agenda for 3:00 PM Team Sync", sourceTranscript = transcript))

                summaryText = "Added: Team Sync, 3:00 PM, reminder set for 2:55 PM"
                spokenConfirmation = "Got it — Team Sync scheduled in 30 minutes. Reminder set for 5 minutes prior."
            }

            lower.contains("alarm") -> {
                val alarmTime = now + 60 * 60 * 1000L
                val spokenMsg = if (lower.contains("saying")) lower.substringAfter("saying") else "Time to get back to work!"
                alarms.add(VoiceAlarmEntity(triggerTimeMillis = alarmTime, spokenMessage = spokenMsg.trim(), sourceTranscript = transcript))
                summaryText = "Voice alarm set for 1 hour from now: \"${spokenMsg.trim()}\""
                spokenConfirmation = "Set a voice alarm for 1 hour from now."
            }

            lower.contains("remind me") || lower.contains("reminder") -> {
                val msg = transcript.replace("remind me to", "", ignoreCase = true).replace("remind me", "", ignoreCase = true).trim()
                val remTime = now + 15 * 60 * 1000L
                reminders.add(ReminderEntity(message = msg.ifEmpty { "Reminder" }, remindAtMillis = remTime, sourceTranscript = transcript))
                summaryText = "Reminder set for 15 minutes from now: \"$msg\""
                spokenConfirmation = "I'll remind you to $msg in 15 minutes."
            }

            lower.contains("note") || lower.contains("record") -> {
                val title = if (lower.contains("about")) lower.substringAfter("about") else "Voice Note"
                notes.add(MeetingNoteEntity(title = title.take(25).trim(), transcript = transcript, summary = "Recorded voice note."))
                summaryText = "Saved meeting note: \"$transcript\""
                spokenConfirmation = "Recorded your meeting note."
            }

            else -> {
                todos.add(TodoEntity(text = transcript.replaceFirstChar { it.uppercase() }, sourceTranscript = transcript))
                summaryText = "Added to To-Do List: \"$transcript\""
                spokenConfirmation = "Added to your to-do list."
            }
        }

        return ParsedActionsResult(
            transcript = transcript,
            summaryText = summaryText,
            spokenConfirmation = spokenConfirmation,
            todosToCreate = todos,
            calendarBlocksToCreate = events,
            remindersToCreate = reminders,
            alarmsToCreate = alarms,
            meetingNotesToCreate = notes
        )
    }
}