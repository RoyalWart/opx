package com.example.opx.audio.fx

/**
 * High-density algorithmic Plate Reverb based on Dattorro / Schroeder tank topology.
 * Provides silky, dense acoustic space simulation with low-pass absorption damping.
 */
class PlateReverb(
    private var sampleRate: Int = 48000
) {
    var isEnabled: Boolean = true

    // Reverb decay time / size: 0.1 to 0.96 feedback
    var decay: Float = 0.65f
        set(value) { field = value.coerceIn(0.1f, 0.96f) }

    // High frequency damping: 0.0 (dark) to 1.0 (bright)
    var damping: Float = 0.45f
        set(value) { field = value.coerceIn(0.0f, 1.0f) }

    // Stereo Width: 0.0 (mono) to 1.0 (super-wide)
    var stereoWidth: Float = 0.85f
        set(value) { field = value.coerceIn(0.0f, 1.0f) }

    // Dry / Wet mix: 0.0 to 1.0
    var mix: Float = 0.20f
        set(value) { field = value.coerceIn(0.0f, 1.0f) }

    // 4 Input Diffusers (All-pass filters)
    private val diff1 = DelayLine((sampleRate * 0.0047f).toInt())
    private val diff2 = DelayLine((sampleRate * 0.0035f).toInt())
    private val diff3 = DelayLine((sampleRate * 0.0127f).toInt())
    private val diff4 = DelayLine((sampleRate * 0.0093f).toInt())

    // Tank Delay Lines
    private val tankL1 = DelayLine((sampleRate * 0.0672f).toInt())
    private val tankL2 = DelayLine((sampleRate * 0.0150f).toInt())
    private val tankR1 = DelayLine((sampleRate * 0.0604f).toInt())
    private val tankR2 = DelayLine((sampleRate * 0.0125f).toInt())

    // Damping filters state
    private var dampStateL = 0.0f
    private var dampStateR = 0.0f

    // Tank cross-feedback states
    private var tankOutL = 0.0f
    private var tankOutR = 0.0f

    fun reset() {
        diff1.reset()
        diff2.reset()
        diff3.reset()
        diff4.reset()
        tankL1.reset()
        tankL2.reset()
        tankR1.reset()
        tankR2.reset()
        dampStateL = 0.0f
        dampStateR = 0.0f
        tankOutL = 0.0f
        tankOutR = 0.0f
    }

    /**
     * All-pass filter processing step.
     */
    private fun processAllPass(delay: DelayLine, input: Float, feedback: Float = 0.5f): Float {
        val delayed = delay.read()
        val v = input - delayed * feedback
        delay.write(v)
        return delayed + v * feedback
    }

    fun process(left: Float, right: Float): Pair<Float, Float> {
        if (!isEnabled || mix <= 0.001f) return Pair(left, right)

        val monoIn = (left + right) * 0.5f

        // 1. Four-stage input diffusion
        var diffOut = processAllPass(diff1, monoIn, 0.70f)
        diffOut = processAllPass(diff2, diffOut, 0.70f)
        diffOut = processAllPass(diff3, diffOut, 0.62f)
        diffOut = processAllPass(diff4, diffOut, 0.62f)

        // 2. Main Tank Cross-Coupled Feedback Loops
        val tankInL = diffOut + tankOutR * decay
        val tankInR = diffOut + tankOutL * decay

        // Left Tank Branch
        val dL1 = tankL1.process(tankInL)
        // High frequency damping absorption
        val dampFactor = 0.20f + (1.0f - damping) * 0.65f
        dampStateL += (dL1 - dampStateL) * (1.0f - dampFactor)
        val apL = processAllPass(tankL2, dampStateL, 0.55f)
        tankOutL = apL

        // Right Tank Branch
        val dR1 = tankR1.process(tankInR)
        dampStateR += (dR1 - dampStateR) * (1.0f - dampFactor)
        val apR = processAllPass(tankR2, dampStateR, 0.55f)
        tankOutR = apR

        // 3. Stereo Extraction with width scaling
        val wetMono = (tankOutL + tankOutR) * 0.5f
        val wetDiff = (tankOutL - tankOutR) * 0.5f * stereoWidth

        val wetLeft = wetMono + wetDiff
        val wetRight = wetMono - wetDiff

        // 4. Dry / Wet blending
        val outLeft = left * (1.0f - mix) + wetLeft * mix
        val outRight = right * (1.0f - mix) + wetRight * mix

        return Pair(outLeft, outRight)
    }

    /**
     * Helper delay line with zero garbage allocation.
     */
    private class DelayLine(val size: Int) {
        val buffer = FloatArray(if (size < 1) 1 else size)
        var index = 0

        fun reset() {
            buffer.fill(0.0f)
            index = 0
        }

        fun read(): Float = buffer[index]

        fun write(sample: Float) {
            buffer[index] = sample
            index = (index + 1) % buffer.size
        }

        fun process(input: Float): Float {
            val out = buffer[index]
            buffer[index] = input
            index = (index + 1) % buffer.size
            return out
        }
    }
}
