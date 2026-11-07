package com.example.opx.audio.fx

import kotlin.math.pow
import kotlin.math.round

/**
 * Lo-Fi Bitcrusher and Downsampling processor.
 * Recreates the crunchy 8-bit / 12-bit digital character of early digital samplers and synths.
 */
class Bitcrusher(
    private var sampleRate: Int = 48000
) {
    var isEnabled: Boolean = true

    // Target bit depth: 2 to 16 bits (default: 16 = no reduction)
    var bitDepth: Int = 16
        set(value) { field = value.coerceIn(2, 16) }

    // Downsampling factor: 1 (normal) to 32 (extreme sample-and-hold lo-fi decimation)
    var downsampleFactor: Int = 1
        set(value) { field = value.coerceIn(1, 32) }

    // Wet/Dry mix: 0.0 (bypass) to 1.0 (fully crushed)
    var mix: Float = 0.0f
        set(value) { field = value.coerceIn(0.0f, 1.0f) }

    // Internal decimation counters per channel
    private var downsampleCounter = 0
    private var heldLeft = 0.0f
    private var heldRight = 0.0f

    fun updateSampleRate(newSampleRate: Int) {
        sampleRate = newSampleRate
    }

    fun reset() {
        downsampleCounter = 0
        heldLeft = 0.0f
        heldRight = 0.0f
    }

    /**
     * Processes a single stereo frame in-place.
     * @return Pair of processed (Left, Right) samples.
     */
    fun process(left: Float, right: Float): Pair<Float, Float> {
        if (!isEnabled || mix <= 0.001f) return Pair(left, right)

        // 1. Sample rate decimation (Sample & Hold)
        if (downsampleCounter++ >= downsampleFactor) {
            downsampleCounter = 1
            heldLeft = left
            heldRight = right
        }

        // 2. Bit depth quantization
        val levels = 2.0.pow(bitDepth.toDouble()).toFloat() - 1.0f
        val crushedLeft = if (bitDepth < 16) {
            round(heldLeft * levels) / levels
        } else {
            heldLeft
        }

        val crushedRight = if (bitDepth < 16) {
            round(heldRight * levels) / levels
        } else {
            heldRight
        }

        // 3. Dry / Wet blending
        val outLeft = left * (1.0f - mix) + crushedLeft * mix
        val outRight = right * (1.0f - mix) + crushedRight * mix

        return Pair(outLeft, outRight)
    }
}
