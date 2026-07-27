package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BentoTabsSection(
    todos: List<TodoEntity>,
    calendarEvents: List<CalendarBlockEntity>,
    reminders: List<ReminderEntity>,
    alarms: List<VoiceAlarmEntity>,
    meetingNotes: List<MeetingNoteEntity>,
    onToggleTodo: (TodoEntity) -> Unit,
    onDeleteTodo: (Int) -> Unit,
    onAddTodoManual: (String) -> Unit,
    onDeleteCalendarBlock: (Int) -> Unit,
    onDeleteReminder: (Int) -> Unit,
    onDeleteAlarm: (Int) -> Unit,
    onDeleteMeetingNote: (Int) -> Unit,
    onTestTts: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Todos, 1: Agenda, 2: Reminders, 3: Notes
    var newTodoText by remember { mutableStateOf("") }

    val tabs = listOf(
        "To-Dos (${todos.size})",
        "Calendar (${calendarEvents.size})",
        "Alarms (${reminders.size + alarms.size})",
        "Notes (${meetingNotes.size})"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BentoSurface, RoundedCornerShape(28.dp))
            .border(1.dp, BentoSurfaceVariant, RoundedCornerShape(28.dp))
            .padding(16.dp)
            .testTag("bento_tabs_section")
    ) {
        // Tab Selector Row
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = BentoPrimary,
            edgePadding = 0.dp,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    modifier = Modifier.testTag("tab_$index")
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == index) BentoPrimary else BentoOnSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content Area by Tab
        when (selectedTab) {
            0 -> {
                // To-Dos View
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Quick add bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTodoText,
                            onValueChange = { newTodoText = it },
                            placeholder = { Text("Add task manually...", fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_todo_input"),
                            shape = CircleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BentoPrimary,
                                unfocusedBorderColor = BentoOutline
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newTodoText.isNotBlank()) {
                                    onAddTodoManual(newTodoText)
                                    newTodoText = ""
                                }
                            },
                            modifier = Modifier
                                .background(BentoPrimary, CircleShape)
                                .size(40.dp)
                                .testTag("add_todo_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Todo", tint = Color.White)
                        }
                    }

                    if (todos.isEmpty()) {
                        Text(
                            text = "No items in your to-do list. Tap Record or type to add one!",
                            fontSize = 12.sp,
                            color = BentoOnSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            todos.forEach { todo ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BentoSurfaceVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = todo.isDone,
                                            onCheckedChange = { onToggleTodo(todo) },
                                            colors = CheckboxDefaults.colors(checkedColor = BentoPrimary)
                                        )
                                        Text(
                                            text = todo.text,
                                            fontSize = 14.sp,
                                            color = if (todo.isDone) BentoOnSurfaceVariant else BentoOnSurface,
                                            textDecoration = if (todo.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        IconButton(onClick = { onDeleteTodo(todo.id) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Calendar Agenda View
                if (calendarEvents.isEmpty()) {
                    Text(
                        text = "No calendar events scheduled today.",
                        fontSize = 12.sp,
                        color = BentoOnSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        calendarEvents.forEach { event ->
                            val sdf = SimpleDateFormat("HH:mm", Locale.US)
                            val start = sdf.format(Date(event.startTimeMillis))
                            val end = sdf.format(Date(event.startTimeMillis + event.durationMinutes * 60 * 1000L))

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = BentoEventCardBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = event.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BentoEventCardText
                                        )
                                        Text(
                                            text = "$start - $end (${event.durationMinutes} min)",
                                            fontSize = 11.sp,
                                            color = BentoPrimary
                                        )
                                    }
                                    IconButton(onClick = { onDeleteCalendarBlock(event.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = BentoEventCardText, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Alarms & Reminders View
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (reminders.isEmpty() && alarms.isEmpty()) {
                        Text(
                            text = "No active reminders or voice alarms.",
                            fontSize = 12.sp,
                            color = BentoOnSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        reminders.forEach { reminder ->
                            val time = SimpleDateFormat("HH:mm", Locale.US).format(Date(reminder.remindAtMillis))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BentoOutline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, tint = BentoPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = reminder.message, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BentoOnSurface)
                                        Text(text = "Reminder at $time", fontSize = 11.sp, color = BentoPrimary)
                                    }
                                    IconButton(onClick = { onDeleteReminder(reminder.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        alarms.forEach { alarm ->
                            val time = SimpleDateFormat("HH:mm", Locale.US).format(Date(alarm.triggerTimeMillis))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = BentoAlarmCardBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = BentoAlarmAccent, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Voice Alarm at $time", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BentoAlarmCardText)
                                        Text(text = "\"${alarm.spokenMessage}\"", fontSize = 11.sp, color = BentoAlarmAccent)
                                    }
                                    IconButton(onClick = { onTestTts(alarm.spokenMessage) }) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Test Speech", tint = BentoAlarmAccent, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = { onDeleteAlarm(alarm.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = BentoAlarmCardText, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                // Meeting Notes View
                if (meetingNotes.isEmpty()) {
                    Text(
                        text = "No meeting notes or voice transcripts saved.",
                        fontSize = 12.sp,
                        color = BentoOnSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        meetingNotes.forEach { note ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BentoSurfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = note.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BentoOnSurface)
                                        IconButton(onClick = { onDeleteMeetingNote(note.id) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Text(text = note.summary, fontSize = 12.sp, color = BentoPrimary, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "\"${note.transcript}\"", fontSize = 11.sp, color = BentoOnSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
