package com.example.opx.audio.fx

import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.tanh

/**
 * Professional Stereo VCA Bus Compressor with Soft Knee and Gain Reduction telemetry.
 */
class MasterCompressor(
    private var sampleRate: Int = 48000
) {
    var isEnabled: Boolean = true

    // Threshold in dB: -40.0 to 0.0 dB
    var thresholdDb: Float = -12.0f
        set(value) { field = value.coerceIn(-40.0f, 0.0f) }

    // Ratio: 1.0 (no compression) to 20.0 (brickwall limit)
    var ratio: Float = 4.0f
        set(value) { field = value.coerceIn(1.0f, 20.0f) }

    // Attack time in ms: 0.1ms to 100ms
    var attackMs: Float = 15.0f
        set(value) {
            field = value.coerceIn(0.1f, 100.0f)
            calculateCoefficients()
        }

    // Release time in ms: 10ms to 1200ms
    var releaseMs: Float = 120.0f
        set(value) {
            field = value.coerceIn(10.0f, 1200.0f)
            calculateCoefficients()
        }

    // Makeup Gain in dB: 0.0 to +18.0 dB
    var makeupGainDb: Float = 3.0f
        set(value) { field = value.coerceIn(0.0f, 18.0f) }

    // Soft knee width in dB
    var kneeWidthDb: Float = 4.0f

    // Live Gain Reduction in dB for the UI meter (0dB = no reduction, -12dB = heavy reduction)
    @Volatile
    var currentGainReductionDb: Float = 0.0f
        private set

    // Envelope detector state
    private var envelopeDb: Float = -96.0f
    private var attackCoeff: Float = 0.0f
    private var releaseCoeff: Float = 0.0f

    init {
        calculateCoefficients()
    }

    fun updateSampleRate(newSampleRate: Int) {
        sampleRate = newSampleRate
        calculateCoefficients()
    }

    private fun calculateCoefficients() {
        attackCoeff = exp(-1.0 / (attackMs * 0.001 * sampleRate)).toFloat()
        releaseCoeff = exp(-1.0 / (releaseMs * 0.001 * sampleRate)).toFloat()
    }

    fun reset() {
        envelopeDb = -96.0f
        currentGainReductionDb = 0.0f
    }

    /**
     * Processes one stereo frame through the bus compressor.
     */
    fun process(left: Float, right: Float): Pair<Float, Float> {
        if (!isEnabled) {
            currentGainReductionDb = 0.0f
            return Pair(left, right)
        }

        // 1. Sidechain peak level detection in dB
        val peakMag = max(Math.abs(left), Math.abs(right)).coerceAtLeast(1e-5f)
        val inputDb = 20.0f * log10(peakMag)

        // 2. Ballistics envelope tracking (Attack / Release)
        val coeff = if (inputDb > envelopeDb) attackCoeff else releaseCoeff
        envelopeDb = coeff * envelopeDb + (1.0f - coeff) * inputDb

        // 3. Static Gain Computer with Soft Knee
        val halfKnee = kneeWidthDb * 0.5f
        val delta = envelopeDb - thresholdDb

        val targetGrDb = when {
            delta <= -halfKnee -> 0.0f // Below knee
            delta >= halfKnee -> (1.0f - 1.0f / ratio) * delta // Above knee
            else -> {
                // Inside soft knee curve (quadratic interpolation)
                val normKnee = (delta + halfKnee) / kneeWidthDb
                (1.0f - 1.0f / ratio) * (normKnee * normKnee * kneeWidthDb * 0.5f)
            }
        }

        currentGainReductionDb = targetGrDb

        // 4. Calculate Linear Gain Reduction & Makeup Gain
        val grLinear = 10.0.pow((-targetGrDb / 20.0).toDouble()).toFloat()
        val makeupLinear = 10.0.pow((makeupGainDb / 20.0).toDouble()).toFloat()
        val totalGain = grLinear * makeupLinear

        // 5. Output with soft analog-style saturation limiter to prevent digital clips
        val outL = tanh((left * totalGain).toDouble()).toFloat()
        val outR = tanh((right * totalGain).toDouble()).toFloat()

        return Pair(outL, outR)
    }
}
