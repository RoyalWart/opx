package com.example.opx.audio.dsp

/**
 * State machine for ADSR Envelope Generator.
 */
enum class EnvelopeState {
    IDLE,
    ATTACK,
    DECAY,
    SUSTAIN,
    RELEASE
}

/**
 * Sample-accurate ADSR envelope generator with exponential curves
 * for natural percussive and sustained musical response.
 */
class AdsrEnvelope(
    private var sampleRate: Int = 48000
) {
    var state: EnvelopeState = EnvelopeState.IDLE
        private set

    // Envelope parameters in seconds
    var attackTimeSec: Float = 0.01f
        set(value) { field = value.coerceAtLeast(0.001f) }

    var decayTimeSec: Float = 0.1f
        set(value) { field = value.coerceAtLeast(0.001f) }

    var sustainLevel: Float = 0.7f
        set(value) { field = value.coerceIn(0.0f, 1.0f) }

    var releaseTimeSec: Float = 0.15f
        set(value) { field = value.coerceAtLeast(0.001f) }

    // Current output level [0.0, 1.0]
    var currentLevel: Float = 0.0f
        private set

    val isActive: Boolean
        get() = state != EnvelopeState.IDLE

    fun updateSampleRate(newSampleRate: Int) {
        sampleRate = newSampleRate
    }

    /**
     * Triggers Note-On.
     */
    fun noteOn() {
        state = EnvelopeState.ATTACK
    }

    /**
     * Triggers Note-Off (enters Release phase).
     */
    fun noteOff() {
        if (state != EnvelopeState.IDLE) {
            state = EnvelopeState.RELEASE
        }
    }

    /**
     * Forces immediate stop of the envelope.
     */
    fun reset() {
        state = EnvelopeState.IDLE
        currentLevel = 0.0f
    }

    /**
     * Computes the next sample envelope coefficient.
     */
    fun process(): Float {
        when (state) {
            EnvelopeState.IDLE -> {
                currentLevel = 0.0f
            }
            EnvelopeState.ATTACK -> {
                val step = 1.0f / (attackTimeSec * sampleRate)
                currentLevel += step
                if (currentLevel >= 1.0f) {
                    currentLevel = 1.0f
                    state = EnvelopeState.DECAY
                }
            }
            EnvelopeState.DECAY -> {
                val step = (1.0f - sustainLevel) / (decayTimeSec * sampleRate)
                currentLevel -= step
                if (currentLevel <= sustainLevel) {
                    currentLevel = sustainLevel
                    state = EnvelopeState.SUSTAIN
                }
            }
            EnvelopeState.SUSTAIN -> {
                currentLevel = sustainLevel
            }
            EnvelopeState.RELEASE -> {
                val step = sustainLevel.coerceAtLeast(currentLevel) / (releaseTimeSec * sampleRate)
                currentLevel -= step
                if (currentLevel <= 0.0001f) {
                    currentLevel = 0.0f
                    state = EnvelopeState.IDLE
                }
            }
        }
        return currentLevel
    }
}
