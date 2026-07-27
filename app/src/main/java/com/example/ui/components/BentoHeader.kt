package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BentoHeader(
    modifier: Modifier = Modifier,
    userInitials: String = "JD",
    onSettingsClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("bento_header"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Logo & Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(BentoPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(45f)
                        .background(Color.White, RoundedCornerShape(2.dp))
                )
            }
            Text(
                text = "Talk to Me",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = BentoOnSurface,
                letterSpacing = (-0.5).sp
            )
        }

        // Right side: Settings + Profile
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Settings gear
            if (onSettingsClick != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(BentoSurface, CircleShape)
                        .clickable { onSettingsClick() }
                        .testTag("settings_gear_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = BentoOnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Profile Avatar Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(BentoPrimaryContainer, CircleShape)
                    .border(1.dp, BentoOutline, CircleShape)
                    .testTag("user_profile_badge"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userInitials,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoOnPrimaryContainer
                )
            }
        }
    }
}
