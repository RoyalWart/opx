package com.example.opx.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.opx.audio.engine.AudioVisualizerBridge
import com.example.ui.theme.ChassisBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OscilloscopeBeam
import com.example.ui.theme.ScreenCanvasBg

/**
 * 60 FPS Real-time Vector Oscilloscope Canvas.
 * Pulls sample snapshots from AudioVisualizerBridge and draws smooth phosphor oscilloscope paths.
 */
@Composable
fun Oscilloscope(
    visualizerBridge: AudioVisualizerBridge,
    activeVoices: Int,
    latencyMs: Float,
    sampleRate: Int,
    waveformLabel: String,
    modifier: Modifier = Modifier
) {
    val sampleBuffer = remember { FloatArray(256) }
    val frameTrigger = remember { mutableStateOf(0L) }

    // High-refresh render loop bound to composition lifecycle
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { nanos ->
                visualizerBridge.readSnapshot(sampleBuffer)
                frameTrigger.value = nanos
            }
        }
    }

    Box(
        modifier = modifier
            .testTag("oscilloscope_screen")
            .clip(RoundedCornerShape(8.dp))
            .background(ScreenCanvasBg)
            .border(1.5.dp, ChassisBorder, RoundedCornerShape(8.dp))
    ) {
        // Waveform Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            // 1. Draw subtle retro vector grid lines
            val gridColor = Color(0xFF181B22)
            val numGridCols = 8
            for (i in 1 until numGridCols) {
                val x = (width / numGridCols) * i
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            val numGridRows = 4
            for (i in 1 until numGridRows) {
                val y = (height / numGridRows) * i
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Center baseline
            drawLine(
                color = Color(0xFF222733),
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 1.5.dp.toPx()
            )

            // 2. Render Oscilloscope Path from audio samples
            val samples = sampleBuffer
            val sampleCount = samples.size
            if (sampleCount > 1) {
                val path = Path()
                val stepX = width / (sampleCount - 1).toFloat()
                val maxAmplitude = centerY * 0.85f

                for (i in 0 until sampleCount) {
                    val x = i * stepX
                    // Clamp sample to avoid drawing outside box
                    val clamped = samples[i].coerceIn(-1.0f, 1.0f)
                    val y = centerY - (clamped * maxAmplitude)
                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                // Phosphor outer glow
                drawPath(
                    path = path,
                    color = NeonCyan.copy(alpha = 0.25f),
                    style = Stroke(
                        width = 5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // High-intensity phosphor beam core
                drawPath(
                    path = path,
                    color = if (activeVoices > 0) OscilloscopeBeam else Color(0xFF3A5F48),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        // Header Telemetry Overlay (Top Left)
        Text(
            text = "DSP: $waveformLabel | $sampleRate Hz",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            color = NeonCyan,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        // Status Telemetry Overlay (Top Right)
        Text(
            text = "LATENCY: ${"%.1f".format(latencyMs)}ms  VOICES: $activeVoices/8",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            color = if (activeVoices > 0) OscilloscopeBeam else Color(0xFF708090),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}
