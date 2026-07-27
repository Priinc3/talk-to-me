package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActionHistoryEntity
import com.example.ui.theme.*

@Composable
fun BentoContextualConfirmationCard(
    actionHistory: ActionHistoryEntity?,
    onUndoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transcriptText = actionHistory?.transcript ?: "Meeting in half an hour"
    val createdDetails = actionHistory?.createdDetails ?: "• Added to Calendar (15:00)\n• Reminder set (14:55)\n• Added to To-Do List"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(BentoHeroDarkBg, RoundedCornerShape(28.dp))
            .padding(20.dp)
            .testTag("bento_contextual_confirmation_card")
    ) {
        // Watermark mic icon
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(72.dp)
                .alpha(0.08f)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "LAST ACTION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoHeroSubtext,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "\"$transcriptText\"",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                    color = BentoHeroDarkText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = createdDetails,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    lineHeight = 16.sp,
                    modifier = Modifier.weight(1f)
                )

                if (actionHistory != null) {
                    Button(
                        onClick = onUndoClick,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BentoPrimary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("undo_action_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = "Undo",
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "UNDO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
