package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun BentoRecordActionBar(
    isListening: Boolean,
    isProcessing: Boolean,
    onRecordClick: () -> Unit,
    onKeyboardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val barHeight1 by infiniteTransition.animateFloat(
        initialValue = 12f, targetValue = 24f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar1"
    )
    val barHeight2 by infiniteTransition.animateFloat(
        initialValue = 20f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar2"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 24.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Waveform & Status Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Waveform Bars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(4.dp).height(if (isListening) barHeight1.dp else 12.dp).background(BentoPrimary, CircleShape))
                    Box(modifier = Modifier.width(4.dp).height(if (isListening) barHeight2.dp else 20.dp).background(BentoPrimary, CircleShape))
                    Box(modifier = Modifier.width(4.dp).height(if (isListening) barHeight1.dp else 14.dp).background(BentoPrimary, CircleShape))
                }

                Text(
                    text = when {
                        isProcessing -> "ANALYZING VOICE INTENT..."
                        isListening -> "LISTENING... TAP TO STOP"
                        else -> "TAP TO RECORD"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoOnSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Right Waveform Bars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(4.dp).height(if (isListening) barHeight1.dp else 14.dp).background(BentoPrimary, CircleShape))
                    Box(modifier = Modifier.width(4.dp).height(if (isListening) barHeight2.dp else 20.dp).background(BentoPrimary, CircleShape))
                    Box(modifier = Modifier.width(4.dp).height(if (isListening) barHeight1.dp else 12.dp).background(BentoPrimary, CircleShape))
                }
            }

            // Main Record Button Row with Keyboard Option
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(48.dp)) // Spacer to balance layout

                // Large Circular Record Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(if (isListening) BentoAlarmAccent else BentoPrimary, CircleShape)
                        .border(4.dp, BentoPrimaryContainer, CircleShape)
                        .clickable { onRecordClick() }
                        .testTag("record_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Record Voice",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Keyboard input button
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(48.dp)
                        .background(BentoSurface, CircleShape)
                        .border(1.dp, BentoSurfaceVariant, CircleShape)
                        .clickable { onKeyboardClick() }
                        .testTag("text_input_mode_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Type Voice Command",
                        tint = BentoPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
