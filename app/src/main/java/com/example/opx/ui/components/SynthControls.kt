package com.example.opx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.opx.audio.dsp.Waveform
import com.example.opx.audio.engine.SynthParameters
import com.example.opx.model.SynthPreset
import com.example.ui.theme.ChassisBorder
import com.example.ui.theme.ChassisCard
import com.example.ui.theme.ChassisSurface
import com.example.ui.theme.CyanBlue
import com.example.ui.theme.MustardOchre
import com.example.ui.theme.SafetyOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun SynthControls(
    params: SynthParameters,
    onParamsChange: (SynthParameters) -> Unit,
    onLoadPreset: (SynthPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .testTag("synth_controls")
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ChassisSurface)
            .border(1.dp, ChassisBorder, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Preset selector chips (Horizontally scrollable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PRESET:",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                modifier = Modifier.padding(end = 4.dp)
            )

            SynthPreset.ALL.forEach { preset ->
                val isSelected = preset.params.waveform == params.waveform &&
                        kotlin.math.abs(preset.params.cutoffHz - params.cutoffHz) < 100f
                Box(
                    modifier = Modifier
                        .testTag("preset_${preset.name.lowercase().replace(" ", "_")}")
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) MustardOchre.copy(alpha = 0.25f) else ChassisCard)
                        .border(
                            1.dp,
                            if (isSelected) MustardOchre else ChassisBorder,
                            RoundedCornerShape(4.dp)
                        )
                        .clickable { onLoadPreset(preset) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = preset.name,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) MustardOchre else TextPrimary
                    )
                }
            }
        }

        // Waveform & Octave Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Waveform Selector
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OSC:",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    modifier = Modifier.padding(end = 2.dp)
                )

                Waveform.values().forEach { wf ->
                    val isSelected = wf == params.waveform
                    Box(
                        modifier = Modifier
                            .testTag("waveform_${wf.shortLabel.lowercase()}")
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) SafetyOrange else ChassisCard)
                            .clickable { onParamsChange(params.copy(waveform = wf)) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = wf.shortLabel,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else TextPrimary
                        )
                    }
                }
            }

            // Octave Selector
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OCTAVE:",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    modifier = Modifier.padding(end = 2.dp)
                )

                listOf(-1, 0, 1).forEach { oct ->
                    val isSelected = oct == params.octaveOffset
                    val label = if (oct > 0) "+$oct" else "$oct"
                    Box(
                        modifier = Modifier
                            .testTag("octave_$oct")
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) CyanBlue else ChassisCard)
                            .clickable { onParamsChange(params.copy(octaveOffset = oct)) }
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else TextPrimary
                        )
                    }
                }
            }
        }
    }
}
