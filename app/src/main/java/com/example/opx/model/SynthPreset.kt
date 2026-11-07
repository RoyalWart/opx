package com.example.opx.model

import com.example.opx.audio.dsp.Waveform
import com.example.opx.audio.engine.SynthParameters

/**
 * Built-in sound presets for the OP-X synthesizer.
 */
data class SynthPreset(
    val name: String,
    val description: String,
    val params: SynthParameters
) {
    companion object {
        val ALL: List<SynthPreset> = listOf(
            SynthPreset(
                name = "Cluster Lead",
                description = "6-Voice virtual analog saw with detune and punchy filter",
                params = SynthParameters(
                    waveform = Waveform.SAW,
                    cutoffHz = 5200f,
                    resonance = 0.35f,
                    fxSend = 0.25f,
                    modulation = 0.65f,
                    attackSec = 0.015f,
                    decaySec = 0.15f,
                    sustainLevel = 0.70f,
                    releaseSec = 0.25f,
                    detuneCents = 8.0f
                )
            ),
            SynthPreset(
                name = "Deep Sub Bass",
                description = "Warm pulse bass with sub-oscillator and low cutoff",
                params = SynthParameters(
                    waveform = Waveform.PULSE,
                    cutoffHz = 950f,
                    resonance = 0.45f,
                    fxSend = 0.40f,
                    modulation = 0.30f,
                    subLevel = 0.75f,
                    attackSec = 0.008f,
                    decaySec = 0.22f,
                    sustainLevel = 0.50f,
                    releaseSec = 0.18f,
                    octaveOffset = -1
                )
            ),
            SynthPreset(
                name = "Analog Strings",
                description = "Lush slow-attack saw pad with silky resonant filter",
                params = SynthParameters(
                    waveform = Waveform.SAW,
                    cutoffHz = 2800f,
                    resonance = 0.20f,
                    fxSend = 0.50f,
                    modulation = 0.70f,
                    attackSec = 0.35f,
                    decaySec = 0.40f,
                    sustainLevel = 0.85f,
                    releaseSec = 0.60f,
                    detuneCents = 14.0f
                )
            ),
            SynthPreset(
                name = "Poly Pluck",
                description = "Sharp percussive triangle/sine pluck for arpeggios",
                params = SynthParameters(
                    waveform = Waveform.TRIANGLE,
                    cutoffHz = 6500f,
                    resonance = 0.50f,
                    fxSend = 0.20f,
                    modulation = 0.20f,
                    attackSec = 0.005f,
                    decaySec = 0.10f,
                    sustainLevel = 0.10f,
                    releaseSec = 0.15f
                )
            ),
            SynthPreset(
                name = "Chiptune Pulse",
                description = "Vintage 8-bit pulse wave with modulated width",
                params = SynthParameters(
                    waveform = Waveform.PULSE,
                    cutoffHz = 12000f,
                    resonance = 0.10f,
                    fxSend = 0.10f,
                    modulation = 0.25f,
                    attackSec = 0.002f,
                    decaySec = 0.08f,
                    sustainLevel = 0.80f,
                    releaseSec = 0.05f
                )
            )
        )
    }
}
