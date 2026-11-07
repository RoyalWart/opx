package com.example.opx.audio.tape

import kotlin.math.floor
import kotlin.math.max

/**
 * Single stereo audio tape track.
 * Supports sound-on-sound overdubbing, linear interpolation for variable-speed scrubbing,
 * and peak calculation for visual tape waveform display.
 */
class TapeTrack(
    val trackId: Int,
    val maxFrames: Int = 48000 * 60 // 60 seconds at 48kHz stereo default
) {
    // Interleaved stereo buffer: [L0, R0, L1, R1, ...]
    val buffer = FloatArray(maxFrames * 2)

    var isArmedForRecording: Boolean = (trackId == 0) // Track 1 armed by default
    var isMuted: Boolean = false
    var isSolo: Boolean = false
    var volume: Float = 0.85f // 0.0 to 1.5
    var pan: Float = 0.5f // 0.0 = Left, 1.0 = Right

    // Overdub feedback factor (0.0 to 1.0). 0.85 = sound-on-sound tape decay
    var overdubDecay: Float = 0.88f

    // Total recorded length in frames
    var recordedFrames: Int = 0

    // Peak levels for VU meter
    @Volatile
    var currentPeakLeft: Float = 0.0f
        private set
    @Volatile
    var currentPeakRight: Float = 0.0f
        private set

    fun clear() {
        buffer.fill(0.0f)
        recordedFrames = 0
        currentPeakLeft = 0.0f
        currentPeakRight = 0.0f
    }

    /**
     * Reads a stereo frame at a fractional frame position using linear interpolation.
     * Supports variable speeds and reverse scrub.
     */
    fun readFrame(framePos: Double, loopLength: Int): Pair<Float, Float> {
        if (isMuted || recordedFrames == 0 || loopLength <= 0) return Pair(0.0f, 0.0f)

        // Wrap framePos within loopLength
        var pos = framePos % loopLength
        if (pos < 0.0) pos += loopLength

        val frame0 = floor(pos).toInt().coerceIn(0, maxFrames - 1)
        val frame1 = ((frame0 + 1) % loopLength).coerceIn(0, maxFrames - 1)
        val frac = (pos - frame0).toFloat()

        val idx0 = frame0 * 2
        val idx1 = frame1 * 2

        val l0 = buffer[idx0]
        val r0 = buffer[idx0 + 1]
        val l1 = buffer[idx1]
        val r1 = buffer[idx1 + 1]

        val left = (l0 + frac * (l1 - l0)) * volume * (1.0f - pan)
        val right = (r0 + frac * (r1 - r0)) * volume * pan

        return Pair(left, right)
    }

    /**
     * Overdubs an incoming audio frame at the integer frame position.
     * Combines existing tape audio with new input using sound-on-sound mixing.
     */
    fun overdubFrame(framePos: Int, inputLeft: Float, inputRight: Float) {
        if (!isArmedForRecording || framePos < 0 || framePos >= maxFrames) return

        val idx = framePos * 2
        val existingLeft = buffer[idx]
        val existingRight = buffer[idx + 1]

        // Sound-on-sound overdub mixing
        val newLeft = (existingLeft * overdubDecay) + inputLeft
        val newRight = (existingRight * overdubDecay) + inputRight

        // Soft-clip to prevent numeric overflow
        buffer[idx] = newLeft.coerceIn(-1.5f, 1.5f)
        buffer[idx + 1] = newRight.coerceIn(-1.5f, 1.5f)

        if (framePos >= recordedFrames) {
            recordedFrames = framePos + 1
        }

        // Update peak levels with fast attack, smooth decay
        currentPeakLeft = max(Math.abs(newLeft), currentPeakLeft * 0.95f)
        currentPeakRight = max(Math.abs(newRight), currentPeakRight * 0.95f)
    }

    /**
     * Computes a condensed waveform overview for UI rendering (e.g. 100 points).
     */
    fun computeWaveformOverview(pointsCount: Int, lengthFrames: Int): FloatArray {
        val result = FloatArray(pointsCount)
        if (recordedFrames == 0 || lengthFrames <= 0) return result

        val effectiveLength = minOf(lengthFrames, recordedFrames)
        val step = max(1, effectiveLength / pointsCount)

        for (i in 0 until pointsCount) {
            val startFrame = i * step
            if (startFrame >= effectiveLength) break
            var peak = 0.0f
            for (f in 0 until step) {
                val frame = startFrame + f
                if (frame >= effectiveLength) break
                val idx = frame * 2
                val mag = max(Math.abs(buffer[idx]), Math.abs(buffer[idx + 1]))
                if (mag > peak) peak = mag
            }
            result[i] = peak.coerceIn(0.0f, 1.0f)
        }
        return result
    }
}
