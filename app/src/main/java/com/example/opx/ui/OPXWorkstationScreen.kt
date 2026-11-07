package com.example.opx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.activity.compose.BackHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.opx.audio.AudioEngine
import com.example.opx.audio.engine.SynthEngine
import com.example.opx.audio.engine.SynthParameters
import com.example.opx.midi.MidiConstants
import com.example.opx.midi.MidiRouter
import com.example.opx.model.SynthPreset
import com.example.opx.ui.components.ColorEncoder
import com.example.opx.ui.components.DrumPadView
import com.example.opx.ui.components.MidiOutView
import com.example.opx.ui.components.MixerView
import com.example.opx.ui.components.OPXMode
import com.example.opx.ui.components.Oscilloscope
import com.example.opx.ui.components.SequencerView
import com.example.opx.ui.components.SynthControls
import com.example.opx.ui.components.TapeDeckView
import com.example.opx.ui.components.TopNavigationBar
import com.example.opx.ui.components.TouchKeyboard
import com.example.ui.theme.ChassisBackground
import com.example.ui.theme.ChassisBorder
import com.example.ui.theme.ChassisCard
import com.example.ui.theme.CyanBlue
import com.example.ui.theme.MustardOchre
import com.example.ui.theme.SafetyOrange
import com.example.ui.theme.SlateGray
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlin.math.pow

@Composable
fun OPXWorkstationScreen(
    audioEngine: AudioEngine,
    synthEngine: SynthEngine,
    midiRouter: MidiRouter,
    modifier: Modifier = Modifier
) {
    var currentMode by remember { mutableStateOf(OPXMode.SYNTH) }
    var isAudioRunning by remember { mutableStateOf(audioEngine.isRunning) }
    var params by remember { mutableStateOf(synthEngine.params) }

    fun updateParams(newParams: SynthParameters) {
        params = newParams
        synthEngine.params = newParams
    }

    // Return to SYNTH home when user presses system Back on any secondary screen
    BackHandler(enabled = currentMode != OPXMode.SYNTH) {
        currentMode = OPXMode.SYNTH
    }

    Scaffold(
        modifier = modifier.testTag("opx_workstation_root"),
        containerColor = ChassisBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Top Navigation Bar
            TopNavigationBar(
                currentMode = currentMode,
                onModeSelect = { currentMode = it },
                isAudioRunning = isAudioRunning,
                onToggleAudio = {
                    if (audioEngine.isRunning) {
                        audioEngine.stop()
                    } else {
                        audioEngine.start()
                    }
                    isAudioRunning = audioEngine.isRunning
                },
                onPanicAllNotesOff = {
                    synthEngine.allNotesOff()
                    midiRouter.panicAllNotesOff()
                }
            )

            // 2. Mode Content
            when (currentMode) {
                OPXMode.SYNTH -> {
                    // Center Vector Display Oscilloscope (60 FPS)
                    Oscilloscope(
                        visualizerBridge = synthEngine.visualizerBridge,
                        activeVoices = synthEngine.activeVoiceCount,
                        latencyMs = audioEngine.estimatedLatencyMs,
                        sampleRate = audioEngine.sampleRate,
                        waveformLabel = params.waveform.displayName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    // 4 Signature OP-X Color Encoders (Blue, Ochre, Slate, Orange)
                    Row(
                        modifier = Modifier
                            .testTag("color_encoders_row")
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ChassisCard)
                            .border(1.dp, ChassisBorder, RoundedCornerShape(8.dp))
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. CYAN BLUE (CC 74): Filter Cutoff
                        val cutoffNorm = ((params.cutoffHz - 20f) / 17980f).coerceIn(0f, 1f)
                        ColorEncoder(
                            label = "Cutoff",
                            valueNormalized = cutoffNorm,
                            displayValue = if (params.cutoffHz >= 1000f) {
                                "${"%.1f".format(params.cutoffHz / 1000f)} kHz"
                            } else {
                                "${params.cutoffHz.toInt()} Hz"
                            },
                            encoderColor = CyanBlue,
                            onValueChange = { norm ->
                                // Logarithmic-style perceptual curve for audio cutoff
                                val hz = 20f + (17980f * norm.pow(2f))
                                updateParams(params.copy(cutoffHz = hz))
                                midiRouter.sendControlChange(MidiConstants.CC_CUTOFF_BLUE, norm)
                            },
                            testTag = "encoder_cutoff_blue"
                        )

                        // 2. MUSTARD OCHRE (CC 71): Filter Resonance Q
                        ColorEncoder(
                            label = "Resonance",
                            valueNormalized = params.resonance / 0.95f,
                            displayValue = "${(params.resonance * 100).toInt()}%",
                            encoderColor = MustardOchre,
                            onValueChange = { norm ->
                                updateParams(params.copy(resonance = norm * 0.95f))
                                midiRouter.sendControlChange(MidiConstants.CC_RESONANCE_OCHRE, norm)
                            },
                            testTag = "encoder_resonance_ochre"
                        )

                        // 3. SLATE GRAY (CC 16): FX Send / Overdrive
                        ColorEncoder(
                            label = "FX Send",
                            valueNormalized = params.fxSend,
                            displayValue = "${(params.fxSend * 100).toInt()}%",
                            encoderColor = SlateGray,
                            onValueChange = { norm ->
                                updateParams(params.copy(fxSend = norm))
                                midiRouter.sendControlChange(MidiConstants.CC_FX_SLATE, norm)
                            },
                            testTag = "encoder_fx_slate"
                        )

                        // 4. SAFETY ORANGE (CC 17): Modulation / PWM
                        ColorEncoder(
                            label = "Mod / PWM",
                            valueNormalized = params.modulation,
                            displayValue = "${(params.modulation * 100).toInt()}%",
                            encoderColor = SafetyOrange,
                            onValueChange = { norm ->
                                updateParams(params.copy(modulation = norm))
                                midiRouter.sendControlChange(MidiConstants.CC_MOD_ORANGE, norm)
                            },
                            testTag = "encoder_mod_orange"
                        )
                    }

                    // Synth Performance Controls (Presets, Waveforms, Octave)
                    SynthControls(
                        params = params,
                        onParamsChange = { updateParams(it) },
                        onLoadPreset = { preset ->
                            updateParams(preset.params)
                        }
                    )
                }
                OPXMode.SEQ -> {
                    SequencerView(
                        sequencer = audioEngine.stepSequencer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                OPXMode.DRUM -> {
                    DrumPadView(
                        drumEngine = audioEngine.drumEngine,
                        onPadTriggeredMidi = { midiNote, velocity ->
                            midiRouter.sendNoteOn(midiNote, velocity)
                            Thread {
                                Thread.sleep(100)
                                midiRouter.sendNoteOff(midiNote)
                            }.start()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                OPXMode.TAPE -> {
                    TapeDeckView(
                        tapeDeck = audioEngine.tapeDeck,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                OPXMode.MIXER -> {
                    MixerView(
                        effectsChain = audioEngine.effectsChain,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                OPXMode.MIDI_OUT -> {
                    MidiOutView(
                        midiRouter = midiRouter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }

            // 3. Bottom Multi-Touch Piano Keyboard
            TouchKeyboard(
                baseMidiNote = 48 + (params.octaveOffset * 12), // C3 + octave
                numWhiteKeys = 14, // 2 full octaves
                onNoteOn = { midiNote ->
                    synthEngine.noteOn(midiNote, 0.9f)
                    midiRouter.sendNoteOn(midiNote, 0.9f)
                },
                onNoteOff = { midiNote ->
                    synthEngine.noteOff(midiNote)
                    midiRouter.sendNoteOff(midiNote)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
