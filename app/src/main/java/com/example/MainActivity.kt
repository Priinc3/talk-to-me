package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.BentoBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current

    val todos by viewModel.todos.collectAsStateWithLifecycle()
    val calendarEvents by viewModel.calendarEvents.collectAsStateWithLifecycle()
    val nextEvent by viewModel.nextEvent.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val nextReminder by viewModel.nextReminder.collectAsStateWithLifecycle()
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val nextAlarm by viewModel.nextAlarm.collectAsStateWithLifecycle()
    val meetingNotes by viewModel.meetingNotes.collectAsStateWithLifecycle()
    val latestAction by viewModel.latestAction.collectAsStateWithLifecycle()

    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val currentTranscript by viewModel.currentTranscript.collectAsStateWithLifecycle()
    val userFeedback by viewModel.userFeedback.collectAsStateWithLifecycle()

    var showPromptDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        } else {
            Toast.makeText(context, "Microphone permission required for voice commands", Toast.LENGTH_SHORT).show()
        }
    }

    if (showSettings) {
        BentoSettingsSection(onBack = { showSettings = false })
        return
    }

    Scaffold(
        bottomBar = {
            BentoRecordActionBar(
                isListening = isListening,
                isProcessing = isProcessing,
                onRecordClick = {
                    if (isListening) {
                        viewModel.stopListening()
                    } else {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            viewModel.startListening()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                onKeyboardClick = {
                    showPromptDialog = true
                }
            )
        },
        containerColor = BentoBackground,
        modifier = Modifier.testTag("main_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BentoBackground)
        ) {
            // Header
            BentoHeader(onSettingsClick = { showSettings = true })

            // Main Bento Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Wide Hero Today's Focus Card
                val nextEventTimeStr = nextEvent?.let {
                    java.text.SimpleDateFormat("h:mm a", java.util.Locale.US).format(java.util.Date(it.startTimeMillis))
                }
                BentoTodayFocusCard(
                    todoCount = todos.count { !it.isDone },
                    eventCount = calendarEvents.size,
                    nextEventTimeStr = nextEventTimeStr
                )

                // 2. Row with Next Event & Active Alarm
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BentoNextEventCard(
                        nextEvent = nextEvent,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    BentoActiveAlarmCard(
                        nextReminder = nextReminder,
                        nextAlarm = nextAlarm,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                // 3. Dark Contextual Confirmation Card (Last Voice Action)
                BentoContextualConfirmationCard(
                    actionHistory = latestAction,
                    onUndoClick = { viewModel.undoLastAction() }
                )

                // 4. Tabs Section for To-Dos, Calendar, Reminders, Notes
                BentoTabsSection(
                    todos = todos,
                    calendarEvents = calendarEvents,
                    reminders = reminders,
                    alarms = alarms,
                    meetingNotes = meetingNotes,
                    onToggleTodo = { viewModel.toggleTodo(it) },
                    onDeleteTodo = { viewModel.deleteTodo(it) },
                    onAddTodoManual = { viewModel.addTodoManual(it) },
                    onDeleteCalendarBlock = { viewModel.deleteCalendarBlock(it) },
                    onDeleteReminder = { viewModel.deleteReminder(it) },
                    onDeleteAlarm = { viewModel.deleteAlarm(it) },
                    onDeleteMeetingNote = { viewModel.deleteMeetingNote(it) },
                    onTestTts = { viewModel.testTts(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showPromptDialog) {
        BentoPromptInputDialog(
            onDismiss = { showPromptDialog = false },
            onSubmitPrompt = { prompt ->
                viewModel.processVoiceCommand(prompt)
            }
        )
    }
}