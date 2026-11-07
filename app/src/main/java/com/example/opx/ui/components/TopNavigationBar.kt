package com.example.opx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ChassisBorder
import com.example.ui.theme.ChassisCard
import com.example.ui.theme.ChassisSurface
import com.example.ui.theme.CyanBlue
import com.example.ui.theme.LedActiveRed
import com.example.ui.theme.OscilloscopeBeam
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

enum class OPXMode(val label: String) {
    SYNTH("SYNTH"),
    SEQ("SEQ"),
    TAPE("TAPE"),
    DRUM("DRUM"),
    MIXER("MIXER"),
    MIDI_OUT("MIDI OUT")
}

/**
 * Top navigation bar modeling the 5 primary OP-X operational modes
 * with audio engine power switch and panic all-notes-off control.
 */
@Composable
fun TopNavigationBar(
    currentMode: OPXMode,
    onModeSelect: (OPXMode) -> Unit,
    isAudioRunning: Boolean,
    onToggleAudio: () -> Unit,
    onPanicAllNotesOff: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .testTag("top_navigation_bar")
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ChassisSurface)
            .border(1.dp, ChassisBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Title & Hardware Tag
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isAudioRunning) OscilloscopeBeam else LedActiveRed)
            )
            Text(
                text = "OP-X",
                color = TextPrimary,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }

        // Mode Navigation Pills
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OPXMode.values().forEach { mode ->
                val isSelected = mode == currentMode
                Box(
                    modifier = Modifier
                        .testTag("mode_${mode.name.lowercase()}")
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) CyanBlue else ChassisCard)
                        .clickable { onModeSelect(mode) }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = mode.label,
                        color = if (isSelected) Color.Black else TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Action Controls (Power & Panic)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Panic Button (stops hanging notes)
            IconButton(
                onClick = onPanicAllNotesOff,
                modifier = Modifier
                    .testTag("panic_button")
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeOff,
                    contentDescription = "Panic: All Notes Off",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Low-Latency Audio Stream Toggle
            IconButton(
                onClick = onToggleAudio,
                modifier = Modifier
                    .testTag("audio_power_toggle")
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = if (isAudioRunning) "Mute Audio Stream" else "Start Audio Stream",
                    tint = if (isAudioRunning) OscilloscopeBeam else LedActiveRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
