package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CalendarBlockEntity
import com.example.data.model.ReminderEntity
import com.example.data.model.VoiceAlarmEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BentoNextEventCard(
    nextEvent: CalendarBlockEntity?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(BentoEventCardBg, RoundedCornerShape(28.dp))
            .border(1.dp, BentoOutline, RoundedCornerShape(28.dp))
            .padding(16.dp)
            .testTag("bento_next_event_card")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = "Next Event",
                    tint = BentoEventCardText,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Info
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "NEXT EVENT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoOnSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Text(
                    text = nextEvent?.title ?: "No events",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val timeStr = if (nextEvent != null) {
                    val sdf = SimpleDateFormat("HH:mm", Locale.US)
                    val start = sdf.format(Date(nextEvent.startTimeMillis))
                    val end = sdf.format(Date(nextEvent.startTimeMillis + nextEvent.durationMinutes * 60 * 1000L))
                    "$start - $end"
                } else {
                    "Tap mic to schedule"
                }
                Text(
                    text = timeStr,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = BentoPrimary
                )
            }
        }
    }
}

@Composable
fun BentoActiveAlarmCard(
    nextReminder: ReminderEntity?,
    nextAlarm: VoiceAlarmEntity?,
    modifier: Modifier = Modifier
) {
    val title = nextAlarm?.spokenMessage ?: nextReminder?.message ?: "No active alarm"
    val quote = if (nextAlarm != null) "\"Voice alarm set\"" else if (nextReminder != null) "\"Reminder set\"" else "\"All clear\""

    val timeStr = when {
        nextAlarm != null -> SimpleDateFormat("HH:mm", Locale.US).format(Date(nextAlarm.triggerTimeMillis))
        nextReminder != null -> SimpleDateFormat("HH:mm", Locale.US).format(Date(nextReminder.remindAtMillis))
        else -> "--:--"
    }

    Box(
        modifier = modifier
            .background(BentoAlarmCardBg, RoundedCornerShape(28.dp))
            .border(1.dp, Color(0xFFF9DEDC), RoundedCornerShape(28.dp))
            .padding(16.dp)
            .testTag("bento_active_alarm_card")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Active Alarm",
                    tint = BentoAlarmCardText,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "ACTIVE ALARM / REMINDER",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoAlarmCardText,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$timeStr • $quote",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = BentoAlarmAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
