package com.example.opx.audio.fx

import kotlin.math.floor
import kotlin.math.sin

enum class DelaySubdivision(val displayName: String, val beatMultiplier: Float) {
    SIXTEENTH("1/16", 0.25f),
    EIGHTH("1/8", 0.50f),
    DOTTED_EIGHTH("1/8 Dotted", 0.75f),
    QUARTER("1/4", 1.00f),
    DOTTED_QUARTER("1/4 Dotted", 1.50f),
    HALF("1/2", 2.00f)
}

/**
 * Vintage Beat-Synced Tape Delay with analog feedback damping and wow/flutter modulation.
 */
class TapeDelay(
    private var sampleRate: Int = 48000
) {
    var isEnabled: Boolean = true

    // Maximum delay buffer: 2.0 seconds stereo
    private val maxDelayFrames = sampleRate * 2
    private val bufferLeft = FloatArray(maxDelayFrames)
    private val bufferRight = FloatArray(maxDelayFrames)
    private var writeIndex = 0

    // Timing
    var isBpmSynced: Boolean = true
    var subdivision: DelaySubdivision = DelaySubdivision.DOTTED_EIGHTH
    var bpm: Float = 120.0f
    var freeTimeMs: Float = 350.0f // If not synced: 10ms to 1200ms

    // Feedback: 0.0 to 0.95
    var feedback: Float = 0.45f
        set(value) { field = value.coerceIn(0.0f, 0.95f) }

    // High frequency damping in feedback loop: 0.0 (dark) to 1.0 (bright)
    var damping: Float = 0.50f
        set(value) { field = value.coerceIn(0.0f, 1.0f) }

    // Tape Wow & Flutter LFO modulation depth: 0.0 to 1.0
    var wowFlutter: Float = 0.15f
        set(value) { field = value.coerceIn(0.0f, 1.0f) }

    // Stereo Ping-Pong mode toggle
    var isPingPong: Boolean = true

    // Dry / Wet mix: 0.0 to 1.0
    var mix: Float = 0.25f
        set(value) { field = value.coerceIn(0.0f, 1.0f) }

    // Feedback damping lowpass filter states
    private var dampedFeedbackLeft = 0.0f
    private var dampedFeedbackRight = 0.0f

    // Wow / Flutter LFO phases
    private var lfoPhase1 = 0.0f
    private var lfoPhase2 = 0.0f

    fun updateSampleRate(newSampleRate: Int) {
        sampleRate = newSampleRate
    }

    fun reset() {
        bufferLeft.fill(0.0f)
        bufferRight.fill(0.0f)
        writeIndex = 0
        dampedFeedbackLeft = 0.0f
        dampedFeedbackRight = 0.0f
    }

    /**
     * Calculates base delay time in frames.
     */
    private fun calculateTargetDelayFrames(): Float {
        return if (isBpmSynced) {
            val secondsPerBeat = 60.0f / bpm
            val delaySeconds = secondsPerBeat * subdivision.beatMultiplier
            (delaySeconds * sampleRate).coerceIn(100f, (maxDelayFrames - 100).toFloat())
        } else {
            (freeTimeMs * 0.001f * sampleRate).coerceIn(100f, (maxDelayFrames - 100).toFloat())
        }
    }

    fun process(left: Float, right: Float): Pair<Float, Float> {
        if (!isEnabled || mix <= 0.001f) return Pair(left, right)

        val baseDelayFrames = calculateTargetDelayFrames()

        // Wow and Flutter LFO modulation (slow drift ~0.5Hz + rapid flutter ~4.5Hz)
        val lfo1 = sin(lfoPhase1) * 12.0f * wowFlutter
        val lfo2 = sin(lfoPhase2) * 4.0f * wowFlutter
        lfoPhase1 += (2.0f * Math.PI.toFloat() * 0.6f) / sampleRate
        lfoPhase2 += (2.0f * Math.PI.toFloat() * 4.2f) / sampleRate
        if (lfoPhase1 > 2.0 * Math.PI) lfoPhase1 -= (2.0 * Math.PI).toFloat()
        if (lfoPhase2 > 2.0 * Math.PI) lfoPhase2 -= (2.0 * Math.PI).toFloat()

        val modulatedDelay = (baseDelayFrames + lfo1 + lfo2).coerceIn(2f, (maxDelayFrames - 2).toFloat())

        // Fractional interpolation read from circular buffer
        val readPos = (writeIndex - modulatedDelay + maxDelayFrames) % maxDelayFrames
        val readIdx0 = floor(readPos).toInt() % maxDelayFrames
        val readIdx1 = (readIdx0 + 1) % maxDelayFrames
        val frac = readPos - floor(readPos)

        val delayedLeft = (bufferLeft[readIdx0] + frac * (bufferLeft[readIdx1] - bufferLeft[readIdx0])).toFloat()
        val delayedRight = (bufferRight[readIdx0] + frac * (bufferRight[readIdx1] - bufferRight[readIdx0])).toFloat()

        // 1-Pole Low-pass filter for tape oxide damping on each repetition
        val alpha = 0.15f + (damping * 0.70f)
        dampedFeedbackLeft += alpha * (delayedLeft - dampedFeedbackLeft)
        dampedFeedbackRight += alpha * (delayedRight - dampedFeedbackRight)

        // Write to delay buffer with feedback (supporting ping-pong routing)
        val fbLeft = if (isPingPong) dampedFeedbackRight else dampedFeedbackLeft
        val fbRight = if (isPingPong) dampedFeedbackLeft else dampedFeedbackRight

        bufferLeft[writeIndex] = (left + fbLeft * feedback).coerceIn(-1.8f, 1.8f)
        bufferRight[writeIndex] = (right + fbRight * feedback).coerceIn(-1.8f, 1.8f)

        writeIndex = (writeIndex + 1) % maxDelayFrames

        // Wet / Dry blend
        val outLeft = left * (1.0f - mix) + delayedLeft * mix
        val outRight = right * (1.0f - mix) + delayedRight * mix

        return Pair(outLeft, outRight)
    }
}
