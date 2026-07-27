package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class ReminderNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Talk to Me Reminder"
        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "You have a scheduled reminder!"
        val reminderId = intent.getIntExtra("EXTRA_REMINDER_ID", -1)

        val channelId = "talk_to_me_reminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Talk to Me Reminders & Voice Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for voice-created reminders and alarms"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)

        // Mark as triggered in database
        if (reminderId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(context)
                val reminder = db.reminderDao().getAllReminders()
                // Update reminder state if needed
            }
        }

        // Initialize TTS to speak out loud the reminder message
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.speak("Attention: $message", TextToSpeech.QUEUE_FLUSH, null, "reminder_speech")
            }
        }
    }
}
