package com.example.opx.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.opx.model.Note
import com.example.opx.sequencer.StepData
import com.example.opx.sequencer.StepSequencer
import com.example.ui.theme.ChassisBorder
import com.example.ui.theme.ChassisCard
import com.example.ui.theme.ChassisSurface
import com.example.ui.theme.CyanBlue
import com.example.ui.theme.LedActiveRed
import com.example.ui.theme.MustardOchre
import com.example.ui.theme.OscilloscopeBeam
import com.example.ui.theme.SafetyOrange
import com.example.ui.theme.SlateGray
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SequencerView(
    sequencer: StepSequencer,
    modifier: Modifier = Modifier
) {
    var selectedTrackIndex by remember { mutableStateOf(0) }
    var selectedStepIndex by remember { mutableStateOf<Int?>(null) }
    var isPlaying by remember { mutableStateOf(sequencer.isPlaying) }
    var isTombolaMode by remember { mutableStateOf(sequencer.isTombolaMode) }

    // Live state tracking for 60fps playhead chasing & Tombola physics
    var currentSteps by remember { mutableStateOf(sequencer.tracks.map { it.currentStepIndex }) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                currentSteps = sequencer.tracks.map { it.currentStepIndex }
                isPlaying = sequencer.isPlaying
            }
        }
    }

    val selectedTrack = sequencer.tracks[selectedTrackIndex]
    val trackColor = TrackColors[selectedTrackIndex]

    Column(
        modifier = modifier
            .testTag("sequencer_view")
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(ChassisSurface)
            .border(1.dp, ChassisBorder, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. Top Sequencer Header: Track Selection, Polymetric Length, Mode Toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Track Selection Pills (1 - 4) with independent lengths
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                sequencer.tracks.forEachIndexed { idx, track ->
                    val isSel = idx == selectedTrackIndex
                    val col = TrackColors[idx]
                    Box(
                        modifier = Modifier
                            .testTag("seq_track_$idx")
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSel) col else ChassisCard)
                            .border(1.dp, if (isSel) col else ChassisBorder, RoundedCornerShape(4.dp))
                            .clickable {
                                selectedTrackIndex = idx
                                selectedStepIndex = null
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "T${idx + 1} (${track.length})",
                            color = if (isSel) Color.Black else TextPrimary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Polymetric Length Selector (Steps 1 to 16)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "LEN:",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = {
                        if (selectedTrack.length > 1) {
                            selectedTrack.length--
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease Length", tint = TextPrimary, modifier = Modifier.size(14.dp))
                }
                Text(
                    text = "${selectedTrack.length}",
                    color = trackColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold
                )
                IconButton(
                    onClick = {
                        if (selectedTrack.length < 16) {
                            selectedTrack.length++
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase Length", tint = TextPrimary, modifier = Modifier.size(14.dp))
                }
            }

            // Tombola Mode Toggle
            Box(
                modifier = Modifier
                    .testTag("tombola_mode_toggle")
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isTombolaMode) MustardOchre else ChassisCard)
                    .clickable {
                        isTombolaMode = !isTombolaMode
                        sequencer.isTombolaMode = isTombolaMode
                        if (isTombolaMode) {
                            sequencer.tombola.isRunning = isPlaying
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Tombola Mode",
                        tint = if (isTombolaMode) Color.Black else MustardOchre,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "TOMBOLA",
                        color = if (isTombolaMode) Color.Black else MustardOchre,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Main Content: Tombola Drum Canvas or 16-Step Matrix
        if (isTombolaMode) {
            TombolaCanvasView(
                tombola = sequencer.tombola,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            // 16-Step Sequencer Matrix
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 16 Step Pads Grid (2 rows of 8 steps)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (row in 0..1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (col in 0..7) {
                                val stepIdx = row * 8 + col
                                val isWithinLength = stepIdx < selectedTrack.length
                                val isCurrentPlayhead = currentSteps[selectedTrackIndex] == stepIdx && isPlaying
                                val step = selectedTrack.steps[stepIdx]
                                val isSelected = selectedStepIndex == stepIdx

                                StepPadButton(
                                    step = step,
                                    isWithinLength = isWithinLength,
                                    isCurrentPlayhead = isCurrentPlayhead,
                                    isSelected = isSelected,
                                    trackColor = trackColor,
                                    onClick = {
                                        if (isWithinLength) {
                                            if (selectedStepIndex == stepIdx) {
                                                // Toggle active state
                                                step.isActive = !step.isActive
                                            } else {
                                                selectedStepIndex = stepIdx
                                                if (!step.isActive) step.isActive = true
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Per-Step Parameter Lock Inspector
                selectedStepIndex?.let { idx ->
                    val step = selectedTrack.steps[idx]
                    StepParameterLockPanel(
                        step = step,
                        trackColor = trackColor,
                        onStepChange = { /* state triggers recompose */ },
                        onClose = { selectedStepIndex = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } ?: run {
                    // Quick Generative Action Bar if no step is opened for editing
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(ChassisCard)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Euclidean Pulse Presets
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF222630))
                                    .clickable { selectedTrack.generateEuclidean(pulses = 5, totalSteps = selectedTrack.length) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text("EUCLID 5", color = CyanBlue, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF222630))
                                    .clickable { selectedTrack.generateEuclidean(pulses = 3, totalSteps = selectedTrack.length) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text("EUCLID 3", color = CyanBlue, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            // Randomize Pattern
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF222630))
                                    .clickable { selectedTrack.randomize(density = 0.5f) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text("RANDOMIZE", color = MustardOchre, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            // Factory Groove
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF222630))
                                    .clickable { sequencer.loadFactoryGroove() }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text("POLY GROOVE", color = SafetyOrange, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Clear Track
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF26181B))
                                .clickable { selectedTrack.clear() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text("CLEAR", color = LedActiveRed, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. Sequencer Transport Bar: Play, BPM Slider, Swing
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ChassisCard)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play / Stop Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = { sequencer.togglePlay() },
                    modifier = Modifier
                        .testTag("seq_play_button")
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) OscilloscopeBeam else Color(0xFF252932))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Sequencer Play",
                        tint = if (isPlaying) Color.Black else OscilloscopeBeam,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = if (isPlaying) "RUNNING" else "STOPPED",
                    color = if (isPlaying) OscilloscopeBeam else TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // BPM Slider Control
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "BPM: ${sequencer.bpm.toInt()}",
                    color = TextPrimary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = sequencer.bpm,
                    onValueChange = { sequencer.bpm = it },
                    valueRange = 60f..200f,
                    modifier = Modifier.width(110.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = CyanBlue,
                        activeTrackColor = CyanBlue,
                        inactiveTrackColor = Color(0xFF1C1F26)
                    )
                )
            }

            // Swing
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "SWING: ${(sequencer.swing * 100).toInt()}%",
                    color = MustardOchre,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = sequencer.swing,
                    onValueChange = { sequencer.swing = it },
                    valueRange = 0f..0.7f,
                    modifier = Modifier.width(80.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MustardOchre,
                        activeTrackColor = MustardOchre,
                        inactiveTrackColor = Color(0xFF1C1F26)
                    )
                )
            }
        }
    }
}

/**
 * Individual Step Pad with LED indicator and parameter lock badge.
 */
@Composable
private fun StepPadButton(
    step: StepData,
    isWithinLength: Boolean,
    isCurrentPlayhead: Boolean,
    isSelected: Boolean,
    trackColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        !isWithinLength -> Color(0xFF14161A) // Dimmed disabled step
        isCurrentPlayhead -> Color.White
        step.isActive -> trackColor
        isSelected -> Color(0xFF2C313C)
        else -> Color(0xFF1E2127)
    }

    val textColor = when {
        !isWithinLength -> Color(0xFF323640)
        isCurrentPlayhead -> Color.Black
        step.isActive -> Color.Black
        else -> TextMuted
    }

    Box(
        modifier = modifier
            .testTag("step_pad_${step.stepIndex}")
            .height(34.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(bgColor)
            .border(
                1.dp,
                if (isSelected) Color.White else if (step.isActive) trackColor else Color(0xFF2B2F38),
                RoundedCornerShape(3.dp)
            )
            .clickable(enabled = isWithinLength, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${step.stepIndex + 1}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = textColor
            )
            if (step.isActive && isWithinLength) {
                Text(
                    text = Note.getNoteName(step.midiNote),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = textColor
                )
            }
        }

        // Parameter Lock indicator badge (Top right red dot)
        if (step.hasParameterLocks && isWithinLength) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(SafetyOrange)
            )
        }
    }
}

/**
 * Parameter Lock Inspector: Edit pitch, cutoff, FX send, and probability for a specific step.
 */
@Composable
private fun StepParameterLockPanel(
    step: StepData,
    trackColor: Color,
    onStepChange: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF15181E))
            .border(1.dp, trackColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "P-LOCK STEP ${step.stepIndex + 1}: ${Note.getNoteName(step.midiNote)}",
                color = trackColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Clear Locks
                if (step.hasParameterLocks) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF281C1F))
                            .clickable {
                                step.clearLocks()
                                onStepChange()
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("CLEAR LOCKS", color = LedActiveRed, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }
                }
                // Done / Close
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(trackColor)
                        .clickable(onClick = onClose)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("DONE", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Pitch & Velocity
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Note Pitch selector
            Column(modifier = Modifier.weight(1f)) {
                Text("PITCH: ${Note.getNoteName(step.midiNote)}", fontSize = 9.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { if (step.midiNote > 24) step.midiNote--; onStepChange() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Pitch Down", tint = TextPrimary, modifier = Modifier.size(12.dp))
                    }
                    IconButton(onClick = { if (step.midiNote < 96) step.midiNote++; onStepChange() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Pitch Up", tint = TextPrimary, modifier = Modifier.size(12.dp))
                    }
                }
            }

            // Locked Cutoff Slider
            Column(modifier = Modifier.weight(2f)) {
                val cutoffVal = step.lockedCutoffHz
                Text(
                    text = if (cutoffVal != null) "LOCK CUTOFF: ${cutoffVal.toInt()} Hz" else "CUTOFF: INHERIT",
                    fontSize = 9.sp,
                    color = CyanBlue,
                    fontFamily = FontFamily.Monospace
                )
                Slider(
                    value = (cutoffVal ?: 4000f),
                    onValueChange = {
                        step.lockedCutoffHz = it
                        onStepChange()
                    },
                    valueRange = 100f..16000f,
                    colors = SliderDefaults.colors(thumbColor = CyanBlue, activeTrackColor = CyanBlue)
                )
            }

            // Locked FX Send Slider
            Column(modifier = Modifier.weight(2f)) {
                val fxVal = step.lockedFxSend
                Text(
                    text = if (fxVal != null) "LOCK FX: ${(fxVal * 100).toInt()}%" else "FX: INHERIT",
                    fontSize = 9.sp,
                    color = SlateGray,
                    fontFamily = FontFamily.Monospace
                )
                Slider(
                    value = (fxVal ?: 0f),
                    onValueChange = {
                        step.lockedFxSend = it
                        onStepChange()
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = SlateGray, activeTrackColor = SlateGray)
                )
            }
        }
    }
}

/**
 * Interactive Tombola Visualizer with rotating tumbler drum and bouncing balls.
 */
@Composable
private fun TombolaCanvasView(
    tombola: com.example.opx.sequencer.TombolaPhysics,
    modifier: Modifier = Modifier
) {
    var frameTrigger by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { nanos ->
                frameTrigger = nanos
            }
        }
    }

    Box(
        modifier = modifier
            .testTag("tombola_canvas_view")
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0C0D10))
            .border(1.dp, ChassisBorder, RoundedCornerShape(6.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val drumRadius = (size.minDimension / 2f) * 0.85f

            // 1. Draw Rotating Tumbler Hexagonal Wall
            val angle = tombola.drumAngleRad
            val numSides = 6
            rotate(angle * (180f / Math.PI.toFloat()), center) {
                for (i in 0 until numSides) {
                    val a1 = (i * (2.0 * Math.PI / numSides)).toFloat()
                    val a2 = ((i + 1) * (2.0 * Math.PI / numSides)).toFloat()
                    val p1 = Offset(center.x + drumRadius * cos(a1), center.y + drumRadius * sin(a1))
                    val p2 = Offset(center.x + drumRadius * cos(a2), center.y + drumRadius * sin(a2))

                    drawLine(
                        color = Color(0xFF3E4554),
                        start = p1,
                        end = p2,
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }

            // 2. Draw Center Pivot
            drawCircle(
                color = Color(0xFF222630),
                radius = 8.dp.toPx(),
                center = center
            )

            // 3. Draw Bouncing Tombola Balls
            val scale = drumRadius / tombola.drumRadius
            for (ball in tombola.balls) {
                val ballPos = Offset(center.x + ball.x * scale, center.y + ball.y * scale)
                drawCircle(
                    color = Color(ball.colorHex),
                    radius = (ball.radius * scale).coerceAtLeast(6.dp.toPx()),
                    center = ballPos
                )
                // Center highlight
                drawCircle(
                    color = Color.White.copy(alpha = 0.45f),
                    radius = (ball.radius * scale * 0.35f),
                    center = ballPos
                )
            }
        }

        // Tombola Balls Count Controls (Bottom Right)
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BALLS: ${tombola.balls.size}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary
            )
            IconButton(onClick = { tombola.removeBall() }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Remove, contentDescription = "Remove Ball", tint = TextPrimary)
            }
            IconButton(onClick = { tombola.addBall() }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Add Ball", tint = TextPrimary)
            }
        }
    }
}
