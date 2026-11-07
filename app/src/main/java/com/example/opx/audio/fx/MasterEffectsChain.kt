package com.example.opx.audio.fx

import kotlin.math.max

/**
 * Global Master DSP Effects Chain.
 * Cascades: Bitcrusher -> Beat-Synced Tape Delay -> Plate Reverb -> Master Bus Compressor.
 * Provides unified routing and parameter linkage with the hardware color encoders.
 */
class MasterEffectsChain(
    val sampleRate: Int = 48000
) {
    val bitcrusher = Bitcrusher(sampleRate)
    val delay = TapeDelay(sampleRate)
    val reverb = PlateReverb(sampleRate)
    val compressor = MasterCompressor(sampleRate)

    var masterVolume: Float = 0.90f
        set(value) { field = value.coerceIn(0.0f, 1.5f) }

    var isBypassed: Boolean = false

    // Telemetry for Master Output Meters
    @Volatile
    var peakOutLeft: Float = 0.0f
        private set
    @Volatile
    var peakOutRight: Float = 0.0f
        private set

    fun updateBpm(bpm: Float) {
        delay.bpm = bpm
    }

    fun reset() {
        bitcrusher.reset()
        delay.reset()
        reverb.reset()
        compressor.reset()
        peakOutLeft = 0.0f
        peakOutRight = 0.0f
    }

    /**
     * Processes an interleaved stereo block [L0, R0, L1, R1, ...] in-place.
     */
    fun processBlock(interleavedBuffer: FloatArray, frameCount: Int) {
        if (isBypassed) return

        var maxL = 0.0f
        var maxR = 0.0f

        for (f in 0 until frameCount) {
            val idx = f * 2
            val rawL = interleavedBuffer[idx]
            val rawR = interleavedBuffer[idx + 1]

            // 1. Bitcrusher (Lo-Fi grit)
            val (crushL, crushR) = bitcrusher.process(rawL, rawR)

            // 2. Beat-Synced Tape Delay
            val (delayL, delayR) = delay.process(crushL, crushR)

            // 3. High-Density Plate Reverb
            val (verbL, verbR) = reverb.process(delayL, delayR)

            // 4. Master Bus Compressor & Limiter
            val (compL, compR) = compressor.process(verbL, verbR)

            // Master Output Level
            val outL = compL * masterVolume
            val outR = compR * masterVolume

            interleavedBuffer[idx] = outL
            interleavedBuffer[idx + 1] = outR

            // Track peaks
            val absL = Math.abs(outL)
            val absR = Math.abs(outR)
            if (absL > maxL) maxL = absL
            if (absR > maxR) maxR = absR
        }

        // Smooth decay on meters
        peakOutLeft = max(maxL, peakOutLeft * 0.90f)
        peakOutRight = max(maxR, peakOutRight * 0.90f)
    }
}
