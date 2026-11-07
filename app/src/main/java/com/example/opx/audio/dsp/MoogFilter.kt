package com.example.opx.audio.dsp

import kotlin.math.PI
import kotlin.math.tan
import kotlin.math.tanh

/**
 * Filter mode options for OP-X.
 */
enum class FilterType {
    LOW_PASS_4POLE, // 24dB/oct resonant ladder
    LOW_PASS_2POLE, // 12dB/oct state variable
    BAND_PASS,
    HIGH_PASS
}

/**
 * Resonant 4-Pole (24dB/oct) virtual analog ladder filter with soft saturation.
 * Provides the classic punchy and self-oscillating character of analog hardware.
 */
class MoogFilter(
    private var sampleRate: Int = 48000
) {
    var filterType: FilterType = FilterType.LOW_PASS_4POLE

    // Cutoff frequency in Hz (20 Hz - 20,000 Hz)
    var cutoffHz: Float = 4000.0f
        set(value) {
            field = value.coerceIn(20.0f, sampleRate * 0.45f)
            calculateCoefficients()
        }

    // Resonance Q factor: 0.0 (no resonance) to 0.98 (self-oscillation)
    var resonance: Float = 0.2f
        set(value) {
            field = value.coerceIn(0.0f, 0.98f)
            calculateCoefficients()
        }

    // Drive / warm saturation
    var drive: Float = 1.0f

    // 4 internal 1-pole state stages
    private var s1 = 0.0f
    private var s2 = 0.0f
    private var s3 = 0.0f
    private var s4 = 0.0f

    private var f = 0.1f
    private var k = 0.0f

    init {
        calculateCoefficients()
    }

    fun updateSampleRate(newSampleRate: Int) {
        sampleRate = newSampleRate
        calculateCoefficients()
    }

    private fun calculateCoefficients() {
        val nyquist = sampleRate * 0.5f
        val normCutoff = (cutoffHz / nyquist).coerceIn(0.001f, 0.95f)
        // Bilinear prewarping approximation for low-pass cutoff tuning
        f = (tan(normCutoff * (PI * 0.5).toFloat()) * 0.5f).coerceIn(0.001f, 0.99f)
        // Feedback resonance gain
        k = 4.0f * resonance
    }

    fun reset() {
        s1 = 0.0f
        s2 = 0.0f
        s3 = 0.0f
        s4 = 0.0f
    }

    /**
     * Filters a single audio sample through the 4-pole resonant ladder.
     */
    fun process(input: Float): Float {
        val drivenInput = tanh(input * drive)
        
        // Negative feedback loop with resonance Q
        val x = drivenInput - k * s4

        // Four cascaded 1-pole integrators
        s1 += f * (x - s1)
        s2 += f * (s1 - s2)
        s3 += f * (s2 - s3)
        s4 += f * (s3 - s4)

        return when (filterType) {
            FilterType.LOW_PASS_4POLE -> s4
            FilterType.LOW_PASS_2POLE -> s2
            FilterType.BAND_PASS -> s2 - s4
            FilterType.HIGH_PASS -> x - s4
        }
    }
}
