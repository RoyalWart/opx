package com.example.opx.audio.engine

import com.example.opx.audio.dsp.Waveform

/**
 * Real-time parameters for the OP-X synthesizer engine.
 * Maps directly to the 4 signature hardware color encoders and performance controls.
 */
data class SynthParameters(
    // --- 4 OP-X COLOR ENCODERS ---
    /** BLUE ENCODER (CC 74): Filter Cutoff [20.0 Hz - 18000.0 Hz] */
    val cutoffHz: Float = 4200.0f,

    /** MUSTARD OCHRE ENCODER (CC 71): Filter Resonance Q [0.0 - 0.95] */
    val resonance: Float = 0.25f,

    /** SLATE GRAY ENCODER (CC 16): FX Send / Drive / Saturation [0.0 - 1.0] */
    val fxSend: Float = 0.15f,

    /** SAFETY ORANGE ENCODER (CC 17): Modulation / PWM / Detune [0.0 - 1.0] */
    val modulation: Float = 0.50f,

    // --- OSCILLATOR CONTROLS ---
    val waveform: Waveform = Waveform.SAW,
    val octaveOffset: Int = 0, // -2, -1, 0, +1, +2
    val pulseWidth: Float = 0.5f,
    val subLevel: Float = 0.0f,
    val detuneCents: Float = 6.0f,

    // --- ENVELOPE CONTROLS ---
    val attackSec: Float = 0.015f,
    val decaySec: Float = 0.12f,
    val sustainLevel: Float = 0.65f,
    val releaseSec: Float = 0.20f,

    // --- MASTER CONTROLS ---
    val masterVolume: Float = 0.85f,
    val polyphonyMode: PolyphonyMode = PolyphonyMode.POLY_8
)

enum class PolyphonyMode(val voiceCount: Int, val displayName: String) {
    MONO(1, "Mono (Legato)"),
    POLY_4(4, "4-Voice Poly"),
    POLY_8(8, "8-Voice Poly")
}
