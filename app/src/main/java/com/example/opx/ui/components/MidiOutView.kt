package com.example.opx.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.opx.midi.MidiConstants
import com.example.opx.midi.MidiLogEntry
import com.example.opx.midi.MidiRouter
import com.example.ui.theme.ChassisBorder
import com.example.ui.theme.ChassisCard
import com.example.ui.theme.ChassisSurface
import com.example.ui.theme.CyanBlue
import com.example.ui.theme.LedActiveRed
import com.example.ui.theme.MustardOchre
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OscilloscopeBeam
import com.example.ui.theme.SafetyOrange
import com.example.ui.theme.SlateGray
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun MidiOutView(
    midiRouter: MidiRouter,
    modifier: Modifier = Modifier
) {
    var logs by remember { mutableStateOf(midiRouter.activityLog) }
    var usbDevices by remember { mutableStateOf(midiRouter.usbMidiManager.connectedDeviceNames) }
    var packetsSent by remember { mutableStateOf(midiRouter.networkBroadcaster.packetsSentCount) }
    var selectedDawProfile by remember { mutableStateOf("LMMS / Ableton") }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                logs = midiRouter.activityLog
                usbDevices = midiRouter.usbMidiManager.connectedDeviceNames
                packetsSent = midiRouter.networkBroadcaster.packetsSentCount
            }
        }
    }

    Row(
        modifier = modifier
            .testTag("midi_out_view")
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(ChassisSurface)
            .border(1.dp, ChassisBorder, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Left Column: USB & Network Config Cards
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. USB-MIDI Hardware Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(ChassisCard)
                    .border(1.dp, if (midiRouter.isUsbEnabled) CyanBlue.copy(alpha = 0.5f) else ChassisBorder, RoundedCornerShape(6.dp))
                    .padding(8.dp),
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
                        Icon(Icons.Default.Usb, contentDescription = "USB MIDI", tint = CyanBlue, modifier = Modifier.size(16.dp))
                        Text("USB-MIDI CLASS COMPLIANT", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyanBlue)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { midiRouter.usbMidiManager.scanAndConnectDevices() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Rescan USB", tint = TextPrimary, modifier = Modifier.size(14.dp))
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (midiRouter.isUsbEnabled) CyanBlue else Color(0xFF222630))
                                .clickable { midiRouter.isUsbEnabled = !midiRouter.isUsbEnabled }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(if (midiRouter.isUsbEnabled) "ON" else "OFF", fontSize = 8.sp, color = if (midiRouter.isUsbEnabled) Color.Black else TextMuted, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Connected Devices List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF131518))
                        .padding(6.dp)
                ) {
                    usbDevices.forEach { devName ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(if (devName.contains("No USB")) TextMuted else OscilloscopeBeam))
                            Text(devName, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = if (devName.contains("No USB")) TextMuted else TextPrimary)
                        }
                    }
                }
            }

            // 2. Wi-Fi Network MIDI Broadcast Card (ipMIDI / UDP)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(ChassisCard)
                    .border(1.dp, if (midiRouter.isNetworkEnabled) MustardOchre.copy(alpha = 0.5f) else ChassisBorder, RoundedCornerShape(6.dp))
                    .padding(8.dp),
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
                        Icon(Icons.Default.Wifi, contentDescription = "Network MIDI", tint = MustardOchre, modifier = Modifier.size(16.dp))
                        Text("WI-FI NETWORK MIDI (ipMIDI)", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MustardOchre)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (midiRouter.isNetworkEnabled) MustardOchre else Color(0xFF222630))
                            .clickable { midiRouter.isNetworkEnabled = !midiRouter.isNetworkEnabled }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(if (midiRouter.isNetworkEnabled) "BROADCAST ON" else "OFF", fontSize = 8.sp, color = if (midiRouter.isNetworkEnabled) Color.Black else TextMuted, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF131518))
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("HOST IP: ${midiRouter.networkBroadcaster.localIpAddress}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                        Text("TARGET: ${midiRouter.networkBroadcaster.broadcastAddressStr}:${midiRouter.networkBroadcaster.port}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("SENT: $packetsSent pkts", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = OscilloscopeBeam, fontWeight = FontWeight.Bold)
                        Text("STANDARD: ipMIDI", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = MustardOchre)
                    }
                }
            }

            // 3. MIDI Channel & DAW Mapping Table
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ChassisCard)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("MIDI CHANNEL: ${midiRouter.defaultChannel + 1}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextPrimary)

                    // Channel selector pills
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        (1..8).forEach { ch ->
                            val isSel = midiRouter.defaultChannel == ch - 1
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isSel) SafetyOrange else Color(0xFF22252C))
                                    .clickable { midiRouter.defaultChannel = ch - 1 }
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text("$ch", fontSize = 8.sp, color = if (isSel) Color.Black else TextPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Section 5.1 Mapping Summary
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF131518))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("DAW CC MAPPINGS (LMMS / ABLETON):", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = SlateGray, fontWeight = FontWeight.Bold)
                    Text("• BLUE ENCODER   -> CC 74 (Cutoff)", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = CyanBlue)
                    Text("• OCHRE ENCODER  -> CC 71 (Resonance)", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = MustardOchre)
                    Text("• SLATE ENCODER  -> CC 16 (FX Send 1)", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = SlateGray)
                    Text("• ORANGE ENCODER -> CC 17 (FX Send 2 / Mod)", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = SafetyOrange)
                }

                // Test Triggers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF202632))
                            .clickable {
                                midiRouter.sendNoteOn(60, 0.9f)
                                Thread {
                                    Thread.sleep(150)
                                    midiRouter.sendNoteOff(60)
                                }.start()
                            }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("TEST C4", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = CyanBlue, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF2A1F22))
                            .clickable { midiRouter.panicAllNotesOff() }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("PANIC ALL OFF", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = LedActiveRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Right Column: Live MIDI Activity Terminal Console
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0C0D10))
                .border(1.dp, ChassisBorder, RoundedCornerShape(6.dp))
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MIDI ACTIVITY MONITOR",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Text(
                    text = "${logs.size} EVENTS",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }

            // Scrolling terminal list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (logs.isEmpty()) {
                    item {
                        Text(
                            text = "Awaiting MIDI activity (play keys or turn encoders)...",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                } else {
                    items(logs) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF121418))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = entry.timestamp,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )

                            Text(
                                text = if (entry.isOutbound) "OUT" else "IN",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (entry.isOutbound) OscilloscopeBeam else MustardOchre
                            )

                            Text(
                                text = entry.description,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = entry.hex,
                                fontSize = 7.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
