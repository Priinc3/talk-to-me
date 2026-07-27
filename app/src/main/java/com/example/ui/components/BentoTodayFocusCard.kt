package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BentoTodayFocusCard(
    todoCount: Int,
    eventCount: Int,
    nextEventTimeStr: String?,
    modifier: Modifier = Modifier
) {
    val dateStr = SimpleDateFormat("MMM dd", Locale.US).format(Date()).uppercase()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(BentoSurface, RoundedCornerShape(28.dp))
            .border(1.dp, BentoSurfaceVariant, RoundedCornerShape(28.dp))
            .padding(20.dp)
            .testTag("bento_today_focus_card")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S FOCUS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoPrimary,
                    letterSpacing = 1.2.sp
                )

                Box(
                    modifier = Modifier
                        .background(Color.White, CircleShape)
                        .border(1.dp, BentoSurfaceVariant, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = dateStr,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = BentoOnSurfaceVariant
                    )
                }
            }

            // Headline Statement
            val headlineText = buildAnnotatedString {
                append("You have ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = BentoPrimary)) {
                    append("$todoCount tasks")
                }
                if (eventCount > 0 && !nextEventTimeStr.isNull_or_blank()) {
                    append(" and an event at ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = BentoPrimary)) {
                        append(nextEventTimeStr)
                    }
                } else if (eventCount > 0) {
                    append(" and ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = BentoPrimary)) {
                        append("$eventCount events")
                    }
                }
                append(".")
            }

            Text(
                text = headlineText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                lineHeight = 30.sp,
                color = BentoOnSurface
            )

            // Progress Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .weight(1f)
                        .background(BentoPrimary, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .weight(1f)
                        .background(if (todoCount > 0 || eventCount > 0) BentoPrimary else BentoSurfaceVariant, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .weight(1f)
                        .background(BentoSurfaceVariant, CircleShape)
                )
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()
