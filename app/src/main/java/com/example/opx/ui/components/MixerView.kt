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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PowerSettingsNew
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.opx.audio.fx.DelaySubdivision
import com.example.opx.audio.fx.MasterEffectsChain
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

@Composable
fun MixerView(
    effectsChain: MasterEffectsChain,
    modifier: Modifier = Modifier
) {
    // Dynamic telemetry updates for meters
    var gainReductionDb by remember { mutableStateOf(0.0f) }
    var peakLeft by remember { mutableStateOf(0.0f) }
    var peakRight by remember { mutableStateOf(0.0f) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                gainReductionDb = effectsChain.compressor.currentGainReductionDb
                peakLeft = effectsChain.peakOutLeft
                peakRight = effectsChain.peakOutRight
            }
        }
    }

    Row(
        modifier = modifier
            .testTag("mixer_view")
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(ChassisSurface)
            .border(1.dp, ChassisBorder, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Left Area: 4 Effects Control Cards (Scrollable)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. TAPE DELAY CARD (Cyan Blue Accent)
            FxCard(
                title = "BEAT-SYNCED TAPE DELAY",
                accentColor = CyanBlue,
                isEnabled = effectsChain.delay.isEnabled,
                onToggle = { effectsChain.delay.isEnabled = !effectsChain.delay.isEnabled }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Subdivisions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SYNC:", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                        DelaySubdivision.values().forEach { sub ->
                            val isSel = effectsChain.delay.subdivision == sub
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isSel) CyanBlue else ChassisCard)
                                    .clickable { effectsChain.delay.subdivision = sub }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = sub.displayName,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.Black else TextPrimary
                                )
                            }
                        }
                    }

                    // Sliders: Feedback & Mix
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var fbVal by remember { mutableStateOf(effectsChain.delay.feedback) }
                        var mixVal by remember { mutableStateOf(effectsChain.delay.mix) }
                        var wowVal by remember { mutableStateOf(effectsChain.delay.wowFlutter) }

                        // Feedback
                        Column(modifier = Modifier.weight(1f)) {
                            Text("FEEDBACK: ${(fbVal * 100).toInt()}%", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                            Slider(
                                value = fbVal,
                                onValueChange = {
                                    fbVal = it
                                    effectsChain.delay.feedback = it
                                },
                                colors = SliderDefaults.colors(thumbColor = CyanBlue, activeTrackColor = CyanBlue)
                            )
                        }

                        // Wow / Flutter
                        Column(modifier = Modifier.weight(1f)) {
                            Text("WOW/FLUTTER: ${(wowVal * 100).toInt()}%", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                            Slider(
                                value = wowVal,
                                onValueChange = {
                                    wowVal = it
                                    effectsChain.delay.wowFlutter = it
                                },
                                colors = SliderDefaults.colors(thumbColor = CyanBlue, activeTrackColor = CyanBlue)
                            )
                        }

                        // Mix
                        Column(modifier = Modifier.weight(1f)) {
                            Text("WET MIX: ${(mixVal * 100).toInt()}%", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = CyanBlue)
                            Slider(
                                value = mixVal,
                                onValueChange = {
                                    mixVal = it
                                    effectsChain.delay.mix = it
                                },
                                colors = SliderDefaults.colors(thumbColor = CyanBlue, activeTrackColor = CyanBlue)
                            )
                        }
                    }
                }
            }

            // 2. HIGH-DENSITY PLATE REVERB CARD (Mustard Ochre Accent)
            FxCard(
                title = "DATTORRO PLATE REVERB",
                accentColor = MustardOchre,
                isEnabled = effectsChain.reverb.isEnabled,
                onToggle = { effectsChain.reverb.isEnabled = !effectsChain.reverb.isEnabled }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var decayVal by remember { mutableStateOf(effectsChain.reverb.decay) }
                    var dampVal by remember { mutableStateOf(effectsChain.reverb.damping) }
                    var mixVal by remember { mutableStateOf(effectsChain.reverb.mix) }

                    // Decay / Space
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SPACE / DECAY: ${(decayVal * 100).toInt()}%", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                        Slider(
                            value = decayVal,
                            onValueChange = {
                                decayVal = it
                                effectsChain.reverb.decay = it
                            },
                            valueRange = 0.1f..0.96f,
                            colors = SliderDefaults.colors(thumbColor = MustardOchre, activeTrackColor = MustardOchre)
                        )
                    }

                    // Damping (Tone)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("DAMPING: ${(dampVal * 100).toInt()}%", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                        Slider(
                            value = dampVal,
                            onValueChange = {
                                dampVal = it
                                effectsChain.reverb.damping = it
                            },
                            colors = SliderDefaults.colors(thumbColor = MustardOchre, activeTrackColor = MustardOchre)
                        )
                    }

                    // Reverb Mix
                    Column(modifier = Modifier.weight(1f)) {
                        Text("WET MIX: ${(mixVal * 100).toInt()}%", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = MustardOchre)
                        Slider(
                            value = mixVal,
                            onValueChange = {
                                mixVal = it
                                effectsChain.reverb.mix = it
                            },
                            colors = SliderDefaults.colors(thumbColor = MustardOchre, activeTrackColor = MustardOchre)
                        )
                    }
                }
            }

            // 3. BITCRUSHER / DOWNSAMPLER CARD (Slate Gray Accent)
            FxCard(
                title = "LO-FI BITCRUSHER & DECIMATOR",
                accentColor = SlateGray,
                isEnabled = effectsChain.bitcrusher.isEnabled,
                onToggle = { effectsChain.bitcrusher.isEnabled = !effectsChain.bitcrusher.isEnabled }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var bitsVal by remember { mutableStateOf(effectsChain.bitcrusher.bitDepth) }
                    var downsampleVal by remember { mutableStateOf(effectsChain.bitcrusher.downsampleFactor) }
                    var mixVal by remember { mutableStateOf(effectsChain.bitcrusher.mix) }

                    // Bit Depth selector pills
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text("BITS: $bitsVal-bit", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            listOf(4, 8, 12, 16).forEach { b ->
                                val isSel = bitsVal == b
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (isSel) SlateGray else ChassisCard)
                                        .clickable {
                                            bitsVal = b
                                            effectsChain.bitcrusher.bitDepth = b
                                        }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text("$b", fontSize = 8.sp, color = if (isSel) Color.Black else TextPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Downsampling Decimation
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text("DOWNSAMPLE: ${downsampleVal}x", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                        Slider(
                            value = downsampleVal.toFloat(),
                            onValueChange = {
                                val intVal = it.toInt()
                                downsampleVal = intVal
                                effectsChain.bitcrusher.downsampleFactor = intVal
                            },
                            valueRange = 1f..16f,
                            colors = SliderDefaults.colors(thumbColor = SlateGray, activeTrackColor = SlateGray)
                        )
                    }

                    // Bitcrusher Mix
                    Column(modifier = Modifier.weight(1f)) {
                        Text("DRIVE MIX: ${(mixVal * 100).toInt()}%", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = SlateGray)
                        Slider(
                            value = mixVal,
                            onValueChange = {
                                mixVal = it
                                effectsChain.bitcrusher.mix = it
                            },
                            colors = SliderDefaults.colors(thumbColor = SlateGray, activeTrackColor = SlateGray)
                        )
                    }
                }
            }

            // 4. MASTER BUS COMPRESSOR CARD (Safety Orange Accent)
            FxCard(
                title = "VCA MASTER BUS COMPRESSOR",
                accentColor = SafetyOrange,
                isEnabled = effectsChain.compressor.isEnabled,
                onToggle = { effectsChain.compressor.isEnabled = !effectsChain.compressor.isEnabled }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Gain Reduction Meter Bar (Horizontal LED meter)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "GR: -${"%.1f".format(gainReductionDb)} dB",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (gainReductionDb > 0.5f) SafetyOrange else TextMuted
                        )
                        // Gain Reduction meter graphic
                        Canvas(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF14161A))
                        ) {
                            val w = size.width
                            val h = size.height
                            val normGr = (gainReductionDb / 18.0f).coerceIn(0.0f, 1.0f)
                            drawRect(
                                color = if (gainReductionDb > 10.0f) LedActiveRed else SafetyOrange,
                                topLeft = Offset(0f, 0f),
                                size = Size(w * normGr, h)
                            )
                        }
                    }

                    // Compressor Controls: Threshold, Ratio, Attack, Release, Makeup
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var threshVal by remember { mutableStateOf(effectsChain.compressor.thresholdDb) }
                        var ratioVal by remember { mutableStateOf(effectsChain.compressor.ratio) }
                        var makeupVal by remember { mutableStateOf(effectsChain.compressor.makeupGainDb) }

                        // Threshold
                        Column(modifier = Modifier.weight(1f)) {
                            Text("THRESH: ${threshVal.toInt()} dB", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                            Slider(
                                value = threshVal,
                                onValueChange = {
                                    threshVal = it
                                    effectsChain.compressor.thresholdDb = it
                                },
                                valueRange = -36f..0f,
                                colors = SliderDefaults.colors(thumbColor = SafetyOrange, activeTrackColor = SafetyOrange)
                            )
                        }

                        // Ratio
                        Column(modifier = Modifier.weight(1f)) {
                            Text("RATIO: ${"%.1f".format(ratioVal)}:1", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                            Slider(
                                value = ratioVal,
                                onValueChange = {
                                    ratioVal = it
                                    effectsChain.compressor.ratio = it
                                },
                                valueRange = 1f..12f,
                                colors = SliderDefaults.colors(thumbColor = SafetyOrange, activeTrackColor = SafetyOrange)
                            )
                        }

                        // Makeup
                        Column(modifier = Modifier.weight(1f)) {
                            Text("MAKEUP: +${makeupVal.toInt()} dB", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = SafetyOrange)
                            Slider(
                                value = makeupVal,
                                onValueChange = {
                                    makeupVal = it
                                    effectsChain.compressor.makeupGainDb = it
                                },
                                valueRange = 0f..12f,
                                colors = SliderDefaults.colors(thumbColor = SafetyOrange, activeTrackColor = SafetyOrange)
                            )
                        }
                    }
                }
            }
        }

        // Right Area: Master Level Fader & Stereo VU Output Strip
        Column(
            modifier = Modifier
                .width(68.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
                .background(ChassisCard)
                .border(1.dp, ChassisBorder, RoundedCornerShape(6.dp))
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("MASTER", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextMuted)

            // Stereo Output Peak Meter Bars
            Row(
                modifier = Modifier
                    .weight(1f)
                    .width(28.dp)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Left Meter
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF101215))
                ) {
                    val h = size.height
                    val barH = (peakLeft.coerceIn(0f, 1.2f) / 1.2f) * h
                    drawRect(
                        color = if (peakLeft > 1.0f) LedActiveRed else OscilloscopeBeam,
                        topLeft = Offset(0f, h - barH),
                        size = Size(size.width, barH)
                    )
                }

                // Right Meter
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF101215))
                ) {
                    val h = size.height
                    val barH = (peakRight.coerceIn(0f, 1.2f) / 1.2f) * h
                    drawRect(
                        color = if (peakRight > 1.0f) LedActiveRed else OscilloscopeBeam,
                        topLeft = Offset(0f, h - barH),
                        size = Size(size.width, barH)
                    )
                }
            }

            // Master Volume Slider & Bypass
            var masterVol by remember { mutableStateOf(effectsChain.masterVolume) }
            Text("${(masterVol * 100).toInt()}%", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)

            // Global Bypass Toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (effectsChain.isBypassed) LedActiveRed else Color(0xFF222630))
                    .clickable { effectsChain.isBypassed = !effectsChain.isBypassed }
                    .padding(horizontal = 4.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (effectsChain.isBypassed) "BYPASS" else "FX ON",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (effectsChain.isBypassed) Color.White else OscilloscopeBeam
                )
            }
        }
    }
}

@Composable
private fun FxCard(
    title: String,
    accentColor: Color,
    isEnabled: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF15171C))
            .border(1.dp, if (isEnabled) accentColor.copy(alpha = 0.45f) else ChassisBorder, RoundedCornerShape(6.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isEnabled) accentColor else Color(0xFF333842))
                )
                Text(
                    text = title,
                    color = if (isEnabled) accentColor else TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // Power toggle pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isEnabled) accentColor else Color(0xFF22252C))
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isEnabled) "ACTIVE" else "OFF",
                    color = if (isEnabled) Color.Black else TextMuted,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Card DSP Controls
        content()
    }
}
