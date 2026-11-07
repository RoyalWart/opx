package com.example.opx.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.opx.audio.tape.TapeDeck
import com.example.opx.audio.tape.TransportState
import com.example.opx.audio.tape.WavExporter
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

val TrackColors = listOf(CyanBlue, MustardOchre, SlateGray, SafetyOrange)

@Composable
fun TapeDeckView(
    tapeDeck: TapeDeck,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var playheadPosSec by remember { mutableStateOf(tapeDeck.currentPositionSeconds) }
    var transportState by remember { mutableStateOf(tapeDeck.transportState) }
    var exportStatusMessage by remember { mutableStateOf<String?>(null) }
    var selectedTrackIndex by remember { mutableStateOf(0) }

    // Continuous UI poll loop for tape playhead animation and VU meters
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                playheadPosSec = tapeDeck.currentPositionSeconds
                transportState = tapeDeck.transportState
            }
        }
    }

    Column(
        modifier = modifier
            .testTag("tape_deck_view")
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(ChassisSurface)
            .border(1.dp, ChassisBorder, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Dual Tape Reel Mechanical Display Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0F1013))
                .border(1.dp, ChassisBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Supply Reel
            TapeReelCanvas(
                isPlaying = tapeDeck.isPlaying,
                speed = tapeDeck.currentSpeed,
                tapePackRatio = 0.70f - (playheadPosSec / tapeDeck.totalDurationSeconds) * 0.40f,
                modifier = Modifier.size(76.dp)
            )

            // Center LCD Display: Timecode, Speed, Overdub Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Timecode
                val mins = (playheadPosSec / 60f).toInt()
                val secs = (playheadPosSec % 60f).toInt()
                val millis = ((playheadPosSec - playheadPosSec.toInt()) * 10f).toInt()
                val timeStr = "%02d:%02d.%01d".format(mins, secs, millis)

                Text(
                    text = timeStr,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (transportState == TransportState.RECORDING) LedActiveRed else OscilloscopeBeam
                )

                // State indicator pill
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusText = when (transportState) {
                        TransportState.RECORDING -> "● OVERDUB REC"
                        TransportState.PLAYING -> "▶ PLAYING"
                        TransportState.PAUSED -> "⏸ PAUSED"
                        TransportState.STOPPED -> "■ STOPPED"
                    }
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (transportState == TransportState.RECORDING) LedActiveRed else TextPrimary
                    )

                    Text(
                        text = "${"%.1f".format(tapeDeck.currentSpeed)}x",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MustardOchre
                    )
                }

                // Interactive Scrub Bar
                val progressFraction = (playheadPosSec / tapeDeck.totalDurationSeconds).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF222630))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                tapeDeck.seekFraction(frac)
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(180.dp * progressFraction)
                            .background(SafetyOrange)
                    )
                }
            }

            // Right Take-up Reel
            TapeReelCanvas(
                isPlaying = tapeDeck.isPlaying,
                speed = tapeDeck.currentSpeed,
                tapePackRatio = 0.30f + (playheadPosSec / tapeDeck.totalDurationSeconds) * 0.40f,
                modifier = Modifier.size(76.dp)
            )
        }

        // 2. 4-Track Multi-Track Strip View
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tapeDeck.tracks.forEachIndexed { index, track ->
                val trackColor = TrackColors[index]
                val isSelected = selectedTrackIndex == index

                Row(
                    modifier = Modifier
                        .testTag("track_row_$index")
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) ChassisCard else Color(0xFF16181C))
                        .border(
                            1.dp,
                            if (isSelected) trackColor else ChassisBorder,
                            RoundedCornerShape(4.dp)
                        )
                        .clickable { selectedTrackIndex = index }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Track Number Badge
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(trackColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Arm / Rec Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (track.isArmedForRecording) LedActiveRed else Color(0xFF282C36))
                            .clickable { track.isArmedForRecording = !track.isArmedForRecording }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (track.isArmedForRecording) "REC ARM" else "ARM",
                            color = if (track.isArmedForRecording) Color.White else TextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Mute & Solo
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (track.isMuted) MustardOchre else Color(0xFF222630))
                                .clickable { track.isMuted = !track.isMuted }
                                .padding(horizontal = 5.dp, vertical = 3.dp)
                        ) {
                            Text("M", color = if (track.isMuted) Color.Black else TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (track.isSolo) CyanBlue else Color(0xFF222630))
                                .clickable { track.isSolo = !track.isSolo }
                                .padding(horizontal = 5.dp, vertical = 3.dp)
                        ) {
                            Text("S", color = if (track.isSolo) Color.Black else TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Track Audio Waveform Strip
                    TrackWaveformCanvas(
                        track = track,
                        playheadFraction = (playheadPosSec / tapeDeck.totalDurationSeconds).coerceIn(0f, 1f),
                        trackColor = trackColor,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    // Track VU Meter Peak
                    TrackVuMeter(
                        peakLeft = track.currentPeakLeft,
                        peakRight = track.currentPeakRight,
                        modifier = Modifier
                            .width(8.dp)
                            .fillMaxHeight()
                    )

                    // Clear Track Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF20232A))
                            .clickable { track.clear() }
                            .padding(horizontal = 4.dp, vertical = 3.dp)
                    ) {
                        Text("CLR", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // 3. Master Transport & Scrub Bar
        Row(
            modifier = Modifier
                .testTag("tape_transport_bar")
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(ChassisCard)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Primary Transport Buttons: Record, Play, Pause, Stop, Brake
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // RECORD
                IconButton(
                    onClick = {
                        if (tapeDeck.isRecording) tapeDeck.pause() else tapeDeck.record()
                    },
                    modifier = Modifier
                        .testTag("tape_rec_button")
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (transportState == TransportState.RECORDING) LedActiveRed else Color(0xFF2A2D35))
                ) {
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = "Tape Record",
                        tint = if (transportState == TransportState.RECORDING) Color.White else LedActiveRed,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // PLAY
                IconButton(
                    onClick = { tapeDeck.play() },
                    modifier = Modifier
                        .testTag("tape_play_button")
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (transportState == TransportState.PLAYING) OscilloscopeBeam else Color(0xFF2A2D35))
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Tape Play",
                        tint = if (transportState == TransportState.PLAYING) Color.Black else OscilloscopeBeam,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // PAUSE
                IconButton(
                    onClick = { tapeDeck.pause() },
                    modifier = Modifier
                        .testTag("tape_pause_button")
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (transportState == TransportState.PAUSED) MustardOchre else Color(0xFF2A2D35))
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Tape Pause",
                        tint = if (transportState == TransportState.PAUSED) Color.Black else MustardOchre,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // STOP
                IconButton(
                    onClick = { tapeDeck.stop() },
                    modifier = Modifier
                        .testTag("tape_stop_button")
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2D35))
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Tape Stop & Rewind",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // BRAKE / TAPE STOP
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (tapeDeck.isTapeDecelerating) SafetyOrange else Color(0xFF2A2D35))
                        .clickable { tapeDeck.tapeStop() }
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "BRAKE",
                        color = if (tapeDeck.isTapeDecelerating) Color.White else SafetyOrange,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Speed Selector & Loop Toggle
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Loop Mode Toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (tapeDeck.isLoopEnabled) CyanBlue else Color(0xFF2A2D35))
                        .clickable { tapeDeck.isLoopEnabled = !tapeDeck.isLoopEnabled }
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Loop,
                            contentDescription = "Loop Toggle",
                            tint = if (tapeDeck.isLoopEnabled) Color.Black else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "LOOP",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (tapeDeck.isLoopEnabled) Color.Black else TextMuted
                        )
                    }
                }

                // Speed buttons (-1x, 0.5x, 1x, 2x)
                listOf(-1.0f to "REV", 0.5f to "0.5x", 1.0f to "1.0x", 2.0f to "2.0x").forEach { (speedVal, label) ->
                    val isCurrent = kotlin.math.abs(tapeDeck.targetSpeed - speedVal) < 0.05f
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isCurrent) MustardOchre else Color(0xFF22252C))
                            .clickable { tapeDeck.setSpeed(speedVal) }
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) Color.Black else TextPrimary
                        )
                    }
                }

                // WAV Stem Export Button
                Box(
                    modifier = Modifier
                        .testTag("export_stems_button")
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF202A35))
                        .border(1.dp, CyanBlue, RoundedCornerShape(4.dp))
                        .clickable {
                            val exportDir = context.getExternalFilesDir("OPX_Stems") ?: context.filesDir
                            val files = WavExporter.exportAllStemsToDirectory(tapeDeck, exportDir)
                            exportStatusMessage = "Exported ${files.size} WAV stems to storage!"
                        }
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export Stems",
                            tint = CyanBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "STEMS",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyanBlue
                        )
                    }
                }
            }
        }

        // Export Status Toast / Notification if triggered
        exportStatusMessage?.let { msg ->
            Text(
                text = msg,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = OscilloscopeBeam,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

/**
 * Animated mechanical tape spool with rotating spokes and variable tape pack thickness.
 */
@Composable
private fun TapeReelCanvas(
    isPlaying: Boolean,
    speed: Float,
    tapePackRatio: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tapeReel")
    val durationMs = if (Math.abs(speed) > 0.05f) (1200f / Math.abs(speed)).toInt().coerceIn(150, 4000) else 1000000
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (speed >= 0f) 360f else -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reelSpin"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.minDimension / 2f - 2.dp.toPx()
        val packRadius = outerRadius * tapePackRatio.coerceIn(0.25f, 0.95f)
        val hubRadius = outerRadius * 0.32f

        // 1. Reel Outer Rim
        drawCircle(
            color = Color(0xFF2C3038),
            radius = outerRadius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // 2. Wound Tape Ribbon Layer (Dark Brown Oxide)
        drawCircle(
            color = Color(0xFF382618),
            radius = packRadius,
            center = center
        )

        // 3. Central Plastic/Metal Hub
        drawCircle(
            color = Color(0xFFD4D8E0),
            radius = hubRadius,
            center = center
        )
        drawCircle(
            color = Color(0xFF1E2128),
            radius = hubRadius * 0.45f,
            center = center
        )

        // 4. Rotating Spokes
        val effectiveAngle = if (isPlaying) rotationAngle else 0f
        rotate(effectiveAngle, center) {
            val numSpokes = 3
            for (i in 0 until numSpokes) {
                val angleRad = (i * (360f / numSpokes)) * (Math.PI / 180f)
                val spokeEnd = Offset(
                    x = center.x + (outerRadius * 0.88f) * cos(angleRad).toFloat(),
                    y = center.y + (outerRadius * 0.88f) * sin(angleRad).toFloat()
                )
                drawLine(
                    color = Color(0xFF525866),
                    start = center,
                    end = spokeEnd,
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

/**
 * Visual waveform rendering for a tape track with moving playhead line.
 */
@Composable
private fun TrackWaveformCanvas(
    track: com.example.opx.audio.tape.TapeTrack,
    playheadFraction: Float,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF0C0D10))
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        // Center baseline
        drawLine(
            color = Color(0xFF1A1D24),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1.dp.toPx()
        )

        // Waveform overview bars
        val points = track.computeWaveformOverview(120, track.maxFrames)
        if (points.isNotEmpty()) {
            val stepX = width / points.size.toFloat()
            for (i in points.indices) {
                val mag = points[i]
                if (mag > 0.01f) {
                    val barHeight = mag * (height * 0.85f)
                    val x = i * stepX
                    drawLine(
                        color = trackColor.copy(alpha = 0.65f),
                        start = Offset(x, centerY - barHeight / 2f),
                        end = Offset(x, centerY + barHeight / 2f),
                        strokeWidth = (stepX * 0.8f).coerceAtLeast(1.dp.toPx())
                    )
                }
            }
        }

        // Active Playhead Marker
        val playheadX = width * playheadFraction
        drawLine(
            color = Color.White,
            start = Offset(playheadX, 0f),
            end = Offset(playheadX, height),
            strokeWidth = 2.dp.toPx()
        )
    }
}

/**
 * Dual stereo VU meter bars.
 */
@Composable
private fun TrackVuMeter(
    peakLeft: Float,
    peakRight: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = (width / 2f) - 0.5.dp.toPx()

        val leftH = (peakLeft.coerceIn(0f, 1f) * height)
        val rightH = (peakRight.coerceIn(0f, 1f) * height)

        // Left Channel
        drawRect(
            color = if (peakLeft > 0.90f) LedActiveRed else OscilloscopeBeam,
            topLeft = Offset(0f, height - leftH),
            size = Size(barWidth, leftH)
        )

        // Right Channel
        drawRect(
            color = if (peakRight > 0.90f) LedActiveRed else OscilloscopeBeam,
            topLeft = Offset(barWidth + 1.dp.toPx(), height - rightH),
            size = Size(barWidth, rightH)
        )
    }
}
