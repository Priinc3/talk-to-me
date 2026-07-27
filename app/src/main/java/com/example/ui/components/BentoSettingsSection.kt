package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInBrowser
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*

@Composable
fun BentoSettingsSection(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val sttState by settingsViewModel.sttDownloadState.collectAsStateWithLifecycle()
    val ttsState by settingsViewModel.ttsDownloadState.collectAsStateWithLifecycle()
    val zenStatus by settingsViewModel.zenAiStatus.collectAsStateWithLifecycle()
    val hasZenKey by settingsViewModel.hasZenApiKey.collectAsStateWithLifecycle()
    val hasGeminiKey by settingsViewModel.hasGeminiApiKey.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BentoBackground)
    ) {
        SettingsHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AiModelStatusCard(zenStatus = zenStatus)

            SttModelCard(
                downloadState = sttState,
                onDownload = { settingsViewModel.downloadSttModel() }
            )

            TtsModelCard(
                downloadState = ttsState,
                onDownload = { settingsViewModel.downloadTtsModel() },
                onOpenGitHub = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rhasspy/piper"))
                    context.startActivity(intent)
                }
            )

            ApiKeysCard(
                hasZenKey = hasZenKey,
                hasGeminiKey = hasGeminiKey
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .testTag("settings_header"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .background(BentoPrimaryContainer, CircleShape)
                .testTag("settings_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = BentoOnPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = "Settings",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = BentoOnSurface,
            letterSpacing = (-0.5).sp
        )
    }
}

@Composable
private fun AiModelStatusCard(zenStatus: ConnectionStatus) {
    BentoCardBase(
        testTag = "settings_ai_model_card",
        header = "AI MODEL STATUS"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = BentoPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "zen.ai",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoOnSurface
                    )
                }
                ConnectionStatusBadge(zenStatus)
            }
            Text(
                text = "Core AI model for intent parsing and voice command processing",
                fontSize = 12.sp,
                color = BentoOnSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun SttModelCard(
    downloadState: DownloadState,
    onDownload: () -> Unit
) {
    BentoCardBase(
        testTag = "settings_stt_model_card",
        header = "SPEECH-TO-TEXT MODEL"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NVIDIA Parakeet v3",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoOnSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = null,
                            tint = BentoOnSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "~1.2 GB",
                            fontSize = 11.sp,
                            color = BentoOnSurfaceVariant
                        )
                    }
                }
                DownloadStatusButton(
                    state = downloadState,
                    onClick = onDownload,
                    testTag = "stt_download_button"
                )
            }

            if (downloadState is DownloadState.Downloading) {
                LinearProgressIndicator(
                    progress = { downloadState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .testTag("stt_download_progress"),
                    color = BentoPrimary,
                    trackColor = BentoSurfaceVariant,
                )
            }

            ModelStatusLabel(downloadState)
        }
    }
}

@Composable
private fun TtsModelCard(
    downloadState: DownloadState,
    onDownload: () -> Unit,
    onOpenGitHub: () -> Unit
) {
    BentoCardBase(
        testTag = "settings_tts_model_card",
        header = "TEXT-TO-SPEECH MODEL"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Piper TTS",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoOnSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = null,
                            tint = BentoOnSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "~200 MB",
                            fontSize = 11.sp,
                            color = BentoOnSurfaceVariant
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenGitHub,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(BentoPrimaryContainer, CircleShape)
                            .testTag("tts_github_link")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInBrowser,
                            contentDescription = "Open GitHub",
                            tint = BentoOnPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DownloadStatusButton(
                        state = downloadState,
                        onClick = onDownload,
                        testTag = "tts_download_button"
                    )
                }
            }

            if (downloadState is DownloadState.Downloading) {
                LinearProgressIndicator(
                    progress = { downloadState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .testTag("tts_download_progress"),
                    color = BentoPrimary,
                    trackColor = BentoSurfaceVariant,
                )
            }

            ModelStatusLabel(downloadState)
        }
    }
}

@Composable
private fun ApiKeysCard(
    hasZenKey: Boolean,
    hasGeminiKey: Boolean
) {
    BentoCardBase(
        testTag = "settings_api_keys_card",
        header = "API KEYS"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ApiKeyRow(
                label = "zen.ai API Key",
                isConfigured = hasZenKey,
                testTag = "zen_api_key_status"
            )
            HorizontalDivider(color = BentoSurfaceVariant, thickness = 0.5.dp)
            ApiKeyRow(
                label = "Gemini API Key (fallback)",
                isConfigured = hasGeminiKey,
                testTag = "gemini_api_key_status"
            )
        }
    }
}

@Composable
private fun BentoCardBase(
    testTag: String,
    header: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BentoSurface, RoundedCornerShape(28.dp))
            .padding(20.dp)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = header,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BentoPrimary,
                letterSpacing = 1.2.sp
            )
            content()
        }
    }
}

@Composable
private fun ConnectionStatusBadge(status: ConnectionStatus) {
    val (label, bg, fg) = when (status) {
        ConnectionStatus.Checking -> Triple("Checking...", BentoSurfaceVariant, BentoOnSurfaceVariant)
        ConnectionStatus.Online -> Triple("Online", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        ConnectionStatus.Offline -> Triple("Offline", BentoAlarmCardBg, BentoAlarmAccent)
        ConnectionStatus.Error -> Triple("Error", BentoAlarmCardBg, BentoAlarmAccent)
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag("zen_connection_status")
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

@Composable
private fun DownloadStatusButton(
    state: DownloadState,
    onClick: () -> Unit,
    testTag: String
) {
    when (state) {
        is DownloadState.Idle -> {
            TextButton(
                onClick = onClick,
                modifier = Modifier.testTag(testTag),
                colors = ButtonDefaults.textButtonColors(contentColor = BentoPrimary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Download", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        is DownloadState.Downloading -> {
            Box(
                modifier = Modifier
                    .background(BentoPrimaryContainer, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag(testTag)
            ) {
                Text(
                    text = "${(state.progress * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoOnPrimaryContainer
                )
            }
        }
        is DownloadState.Ready -> {
            Box(
                modifier = Modifier
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag(testTag)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Ready",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
        is DownloadState.Error -> {
            Box(
                modifier = Modifier
                    .background(BentoAlarmCardBg, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag(testTag)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = BentoAlarmAccent,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Retry",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoAlarmAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelStatusLabel(state: DownloadState) {
    val (label, color) = when (state) {
        is DownloadState.Idle -> "Not downloaded" to BentoOnSurfaceVariant
        is DownloadState.Downloading -> "Downloading..." to BentoPrimary
        is DownloadState.Ready -> "Ready" to Color(0xFF2E7D32)
        is DownloadState.Error -> "Error" to BentoAlarmAccent
    }
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = color
    )
}

@Composable
private fun ApiKeyRow(
    label: String,
    isConfigured: Boolean,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = BentoOnSurface
        )
        val (badgeLabel, bg, fg) = if (isConfigured) {
            Triple("Configured", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        } else {
            Triple("Not set", BentoSurfaceVariant, BentoOnSurfaceVariant)
        }
        Box(
            modifier = Modifier
                .background(bg, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .testTag(testTag)
        ) {
            Text(
                text = badgeLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = fg
            )
        }
    }
}