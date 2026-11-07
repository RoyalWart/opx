package com.example.opx.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.opx.audio.drum.DrumEngine
import com.example.ui.theme.ChassisBorder
import com.example.ui.theme.ChassisCard
import com.example.ui.theme.ChassisSurface
import com.example.ui.theme.CyanBlue
import com.example.ui.theme.MustardOchre
import com.example.ui.theme.SafetyOrange
import com.example.ui.theme.SlateGray
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun DrumPadView(
    drumEngine: DrumEngine,
    onPadTriggeredMidi: ((Int, Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedPadIndex by remember { mutableStateOf(0) }
    val selectedVoice = drumEngine.voices[selectedPadIndex]
    val padColors = listOf(CyanBlue, MustardOchre, SlateGray, SafetyOrange, CyanBlue, MustardOchre, SlateGray, SafetyOrange)

    Column(
        modifier = modifier
            .testTag("drum_pad_view")
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(ChassisSurface)
            .border(1.dp, ChassisBorder, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. 8 MPC-Style Drum Pads (2 rows of 4 pads)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (row in 0..1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (col in 0..3) {
                        val padIdx = row * 4 + col
                        val voice = drumEngine.voices[padIdx]
                        val padColor = padColors[padIdx]
                        val isSelected = selectedPadIndex == padIdx
                        var isPressed by remember { mutableStateOf(false) }

                        val animBgColor by animateColorAsState(
                            targetValue = if (isPressed) Color.White else if (isSelected) padColor.copy(alpha = 0.25f) else ChassisCard,
                            animationSpec = tween(durationMillis = 60),
                            label = "padPress"
                        )

                        Box(
                            modifier = Modifier
                                .testTag("drum_pad_$padIdx")
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .background(animBgColor)
                                .border(
                                    1.5.dp,
                                    if (isSelected) padColor else ChassisBorder,
                                    RoundedCornerShape(6.dp)
                                )
                                .pointerInput(padIdx) {
                                    detectTapGestures(
                                        onPress = {
                                            isPressed = true
                                            selectedPadIndex = padIdx
                                            // Vertical position determines velocity
                                            val normY = (it.y / size.height).coerceIn(0.2f, 1.0f)
                                            val vel = 0.5f + (normY * 0.5f)
                                            drumEngine.triggerPad(padIdx, vel)
                                            onPadTriggeredMidi?.invoke(36 + padIdx, vel)
                                            tryAwaitRelease()
                                            isPressed = false
                                        }
                                    )
                                }
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "PAD ${padIdx + 1}",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isPressed) Color.Black else padColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = voice.type.displayName,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isPressed) Color.Black else TextPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Pad Sample Parameter Editor
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(ChassisCard)
                .border(1.dp, ChassisBorder, RoundedCornerShape(6.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EDIT: PAD ${selectedPadIndex + 1} (${selectedVoice.type.displayName})",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = padColors[selectedPadIndex]
                )

                Text(
                    text = "TRIGGER VELOCITY: 100%",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }

            // Parameter Sliders: Pitch, Decay, Level
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var pitchVal by remember(selectedPadIndex) { mutableStateOf(selectedVoice.pitch) }
                var decayVal by remember(selectedPadIndex) { mutableStateOf(selectedVoice.decaySec) }
                var levelVal by remember(selectedPadIndex) { mutableStateOf(selectedVoice.level) }

                // Pitch
                Column(modifier = Modifier.weight(1f)) {
                    Text("PITCH: ${pitchVal.toInt()} Hz", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                    Slider(
                        value = pitchVal,
                        onValueChange = {
                            pitchVal = it
                            selectedVoice.pitch = it
                        },
                        valueRange = 30f..10000f,
                        colors = SliderDefaults.colors(thumbColor = padColors[selectedPadIndex], activeTrackColor = padColors[selectedPadIndex])
                    )
                }

                // Decay Time
                Column(modifier = Modifier.weight(1f)) {
                    Text("DECAY: ${"%.2f".format(decayVal)}s", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                    Slider(
                        value = decayVal,
                        onValueChange = {
                            decayVal = it
                            selectedVoice.decaySec = it
                        },
                        valueRange = 0.03f..1.2f,
                        colors = SliderDefaults.colors(thumbColor = padColors[selectedPadIndex], activeTrackColor = padColors[selectedPadIndex])
                    )
                }

                // Level / Volume
                Column(modifier = Modifier.weight(1f)) {
                    Text("LEVEL: ${(levelVal * 100).toInt()}%", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                    Slider(
                        value = levelVal,
                        onValueChange = {
                            levelVal = it
                            selectedVoice.level = it
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = padColors[selectedPadIndex], activeTrackColor = padColors[selectedPadIndex])
                    )
                }
            }
        }
    }
}
