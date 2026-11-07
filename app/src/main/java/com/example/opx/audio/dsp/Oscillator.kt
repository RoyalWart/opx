package com.example.opx.audio.dsp

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Waveform types supported by the OP-X synthesizer engine.
 */
enum class Waveform(val displayName: String, val shortLabel: String) {
    SAW("Sawtooth", "SAW"),
    PULSE("Pulse / Square", "PUL"),
    TRIANGLE("Triangle", "TRI"),
    SINE("Sine", "SIN"),
    NOISE("White Noise", "NOI")
}

/**
 * High-performance virtual analog oscillator with PolyBLEP anti-aliasing.
 *
 * PolyBLEP (Polynomial Bandlimited Step) reduces high-frequency aliasing
 * near the Nyquist frequency, providing warm, analog-style tone.
 */
class Oscillator(
    var waveform: Waveform = Waveform.SAW,
    var sampleRate: Int = 48000
) {
    // Current phase in range [0.0, 1.0)
    var phase: Float = 0.0f
    
    // Phase increment per audio frame
    private var phaseIncrement: Float = 0.0f

    // Pulse width modulation: 0.1 to 0.9 (0.5 = pure square wave)
    var pulseWidth: Float = 0.5f

    // Sub-oscillator mix and octave (-1 or -2 octaves)
    var subLevel: Float = 0.0f
    var subOctave: Int = 1 // 1 for -1 octave, 2 for -2 octaves
    private var subPhase: Float = 0.0f

    // Noise generator state
    private val rng = Random(System.nanoTime())
    private var lastNoise: Float = 0.0f

    /**
     * Updates oscillator pitch frequency.
     */
    fun setFrequency(freqHz: Float) {
        val clampedFreq = freqHz.coerceIn(10.0f, sampleRate * 0.45f)
        phaseIncrement = clampedFreq / sampleRate.toFloat()
    }

    /**
     * Resets the oscillator phase.
     */
    fun resetPhase() {
        phase = 0.0f
        subPhase = 0.0f
    }

    /**
     * Computes the next raw audio sample and increments internal phase.
     * @param modPhase Optional phase modulation input for FM/cluster synthesis.
     */
    fun nextSample(modPhase: Float = 0.0f): Float {
        val effectivePhase = (phase + modPhase).let { p ->
            val wrapped = p % 1.0f
            if (wrapped < 0f) wrapped + 1.0f else wrapped
        }

        var sample = when (waveform) {
            Waveform.SINE -> {
                sin(effectivePhase * 2.0f * PI.toFloat())
            }
            Waveform.SAW -> {
                // Naive saw: 2 * phase - 1
                var s = 2.0f * effectivePhase - 1.0f
                // PolyBLEP correction at discontinuity (phase = 0)
                s -= polyBlep(effectivePhase, phaseIncrement)
                s
            }
            Waveform.PULSE -> {
                val pw = pulseWidth.coerceIn(0.05f, 0.95f)
                var s = if (effectivePhase < pw) 1.0f else -1.0f
                // PolyBLEP correction at both transitions (0 and pw)
                s += polyBlep(effectivePhase, phaseIncrement)
                s -= polyBlep((effectivePhase + 1.0f - pw) % 1.0f, phaseIncrement)
                s
            }
            Waveform.TRIANGLE -> {
                // Integrates anti-aliased square or continuous triangle
                var s = 4.0f * kotlin.math.abs(effectivePhase - 0.5f) - 1.0f
                s
            }
            Waveform.NOISE -> {
                // Pinkish/filtered white noise
                val white = rng.nextFloat() * 2.0f - 1.0f
                lastNoise = 0.8f * lastNoise + 0.2f * white
                lastNoise
            }
        }

        // Sub-oscillator generation (-1 or -2 octaves square wave)
        if (subLevel > 0.001f) {
            val subInc = phaseIncrement / (1 shl subOctave)
            subPhase += subInc
            if (subPhase >= 1.0f) subPhase -= 1.0f
            val subSample = if (subPhase < 0.5f) 1.0f else -1.0f
            sample = sample * (1.0f - subLevel * 0.5f) + subSample * subLevel * 0.5f
        }

        // Advance primary phase
        phase += phaseIncrement
        if (phase >= 1.0f) {
            phase -= 1.0f
        }

        return sample
    }

    /**
     * PolyBLEP (Polynomial Bandlimited Step) residual function.
     * Smooths discontinuities in sharp waveforms to suppress Nyquist aliasing.
     */
    private fun polyBlep(t: Float, dt: Float): Float {
        if (dt <= 0.0f) return 0.0f
        if (t < dt) {
            val normT = t / dt
            return normT + normT - normT * normT - 1.0f
        } else if (t > 1.0f - dt) {
            val normT = (t - 1.0f) / dt
            return normT * normT + normT + normT + 1.0f
        }
        return 0.0f
    }
}
