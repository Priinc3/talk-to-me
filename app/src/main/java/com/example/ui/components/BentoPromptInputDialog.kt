package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun BentoPromptInputDialog(
    onDismiss: () -> Unit,
    onSubmitPrompt: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    val presetPrompts = listOf(
        "Meeting in half an hour",
        "Remind me to call Mom at 5 PM",
        "Add todo review project deck",
        "Set alarm in 1 hour saying wake up"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = BentoBackground,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("prompt_input_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Type Spoken Command",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoOnSurface
                )

                Text(
                    text = "Simulate voice input for the intent parser:",
                    fontSize = 12.sp,
                    color = BentoOnSurfaceVariant
                )

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("e.g. There's a meeting in half an hour") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("prompt_text_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BentoPrimary,
                        unfocusedBorderColor = BentoOutline
                    )
                )

                // Quick Presets
                Text(
                    text = "Quick Presets:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoPrimary
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presetPrompts.forEach { preset ->
                        OutlinedButton(
                            onClick = { text = preset },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "\"$preset\"",
                                fontSize = 12.sp,
                                color = BentoOnSurface
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = BentoOnSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (text.isNotBlank()) {
                                onSubmitPrompt(text.trim())
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                        modifier = Modifier.testTag("submit_prompt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Process", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
