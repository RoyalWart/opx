package com.example.opx.sequencer

/**
 * Data model for a single step in the 16-step sequencer.
 * Supports per-step parameter locking (P-Locks) for Pitch, Cutoff, Resonance, and FX Send,
 * as well as probabilistic trigger chance.
 */
data class StepData(
    val stepIndex: Int,
    var isActive: Boolean = false,
    var midiNote: Int = 60, // C4
    var velocity: Float = 0.85f,
    var gateRatio: Float = 0.65f, // 0.1 to 1.0 (fraction of step duration)
    
    // Per-Step Parameter Locks (null means inherit global synth encoder parameter)
    var lockedCutoffHz: Float? = null,
    var lockedResonance: Float? = null,
    var lockedFxSend: Float? = null,
    var lockedModulation: Float? = null,

    // Trigger probability (1.0 = 100% trigger, 0.5 = 50% chance for humanized generative patterns)
    var probability: Float = 1.0f
) {
    val hasParameterLocks: Boolean
        get() = lockedCutoffHz != null || lockedResonance != null || lockedFxSend != null || lockedModulation != null

    fun clearLocks() {
        lockedCutoffHz = null
        lockedResonance = null
        lockedFxSend = null
        lockedModulation = null
    }

    fun duplicate(): StepData {
        return copy()
    }
}
