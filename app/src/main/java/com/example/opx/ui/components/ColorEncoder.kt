package com.example.opx.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ChassisCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Signature OP-X Color Encoder dial.
 * Supports fluid vertical drag gestures with responsive arc visualizer.
 */
@Composable
fun ColorEncoder(
    label: String,
    valueNormalized: Float, // [0.0 - 1.0]
    displayValue: String,
    encoderColor: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    dialSize: Dp = 68.dp,
    testTag: String = "encoder_$label"
) {
    val currentVal = rememberUpdatedState(valueNormalized)
    val onValueChangeUpdated = rememberUpdatedState(onValueChange)

    Column(
        modifier = modifier.testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Encoder Dial with gesture listener
        Box(
            modifier = Modifier
                .size(dialSize)
                .clip(CircleShape)
                .background(ChassisCard)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Dragging upwards increases value, downwards decreases
                        val delta = -dragAmount.y / 240.0f
                        val nextVal = (currentVal.value + delta).coerceIn(0.0f, 1.0f)
                        onValueChangeUpdated.value(nextVal)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(dialSize)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = (size.minDimension / 2f) - 6.dp.toPx()

                // Background track (270 degrees sweep)
                val startAngle = 135f
                val totalSweep = 270f
                drawArc(
                    color = Color(0xFF1E2127),
                    startAngle = startAngle,
                    sweepAngle = totalSweep,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )

                // Colored value arc
                val activeSweep = totalSweep * currentVal.value
                drawArc(
                    color = encoderColor,
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )

                // Inner Knob Body
                val knobRadius = radius - 6.dp.toPx()
                drawCircle(
                    color = Color(0xFF282B33),
                    radius = knobRadius,
                    center = center
                )
                drawCircle(
                    color = encoderColor.copy(alpha = 0.25f),
                    radius = knobRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Pointer indicator line
                val currentAngleRad = (startAngle + activeSweep) * (PI.toFloat() / 180f)
                val indicatorStart = Offset(
                    x = center.x + (knobRadius * 0.35f) * cos(currentAngleRad),
                    y = center.y + (knobRadius * 0.35f) * sin(currentAngleRad)
                )
                val indicatorEnd = Offset(
                    x = center.x + (knobRadius * 0.85f) * cos(currentAngleRad),
                    y = center.y + (knobRadius * 0.85f) * sin(currentAngleRad)
                )
                drawLine(
                    color = encoderColor,
                    start = indicatorStart,
                    end = indicatorEnd,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Center colored core dot
                drawCircle(
                    color = encoderColor,
                    radius = 3.dp.toPx(),
                    center = center
                )
            }
        }

        // Label
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = encoderColor,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )

        // Formatted Value
        Text(
            text = displayValue,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace
        )
    }
}
