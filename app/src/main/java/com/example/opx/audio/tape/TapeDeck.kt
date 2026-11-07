package com.example.opx.audio.tape

import kotlin.math.floor
import kotlin.math.max

enum class TransportState {
    STOPPED,
    PLAYING,
    RECORDING,
    PAUSED
}

/**
 * 4-Track Virtual Tape Workstation Deck.
 * Manages 4 synchronized stereo tracks, sound-on-sound overdubbing,
 * variable-speed scrub playback (-200% to +200%), and loop cycling.
 */
class TapeDeck(
    val sampleRate: Int = 48000,
    val maxTapeSeconds: Int = 60
) {
    val totalFrames: Int = sampleRate * maxTapeSeconds
    val tracks: Array<TapeTrack> = Array(4) { id -> TapeTrack(id, totalFrames) }

    var transportState: TransportState = TransportState.STOPPED
        private set

    // Playhead position in fractional frames
    var playheadFrame: Double = 0.0

    // Loop bounds (default: 8 bars at 120 BPM = 16 seconds)
    var isLoopEnabled: Boolean = true
    var loopStartFrame: Int = 0
    var loopEndFrame: Int = sampleRate * 16 // 16 seconds default loop

    // Variable speed (-2.0 to +2.0). 1.0 = normal, 0.5 = half speed, -1.0 = reverse
    var targetSpeed: Float = 1.0f
    var currentSpeed: Float = 1.0f

    // Inertia simulation for tape stop/start motor deceleration
    var isTapeDecelerating: Boolean = false

    val isPlaying: Boolean
        get() = transportState == TransportState.PLAYING || transportState == TransportState.RECORDING

    val isRecording: Boolean
        get() = transportState == TransportState.RECORDING

    val currentPositionSeconds: Float
        get() = (playheadFrame.toFloat() / sampleRate.toFloat())

    val totalDurationSeconds: Float
        get() = (loopEndFrame.toFloat() / sampleRate.toFloat())

    fun play() {
        isTapeDecelerating = false
        currentSpeed = targetSpeed
        transportState = TransportState.PLAYING
    }

    fun record() {
        isTapeDecelerating = false
        currentSpeed = targetSpeed
        transportState = TransportState.RECORDING
    }

    fun pause() {
        transportState = TransportState.PAUSED
    }

    fun stop() {
        transportState = TransportState.STOPPED
        isTapeDecelerating = false
        playheadFrame = loopStartFrame.toDouble()
    }

    /**
     * Simulates the iconic OP-1 tape stop motor brake.
     */
    fun tapeStop() {
        isTapeDecelerating = true
    }

    fun seekFraction(fraction: Float) {
        val loopLen = max(1, loopEndFrame - loopStartFrame)
        playheadFrame = loopStartFrame + (fraction.coerceIn(0.0f, 1.0f) * loopLen).toDouble()
    }

    fun setSpeed(speed: Float) {
        targetSpeed = speed.coerceIn(-2.0f, 2.0f)
        if (!isTapeDecelerating) {
            currentSpeed = targetSpeed
        }
    }

    fun clearTrack(trackIndex: Int) {
        if (trackIndex in 0..3) {
            tracks[trackIndex].clear()
        }
    }

    fun clearAll() {
        tracks.forEach { it.clear() }
        stop()
    }

    /**
     * Real-time audio DSP loop processing.
     * Overdubs input audio to armed tracks and mixes all 4 tracks to output.
     *
     * @param inputStereo Interleaved input samples [L0, R0, L1, R1, ...] (from Synth)
     * @param outputStereo Interleaved output samples to mix with
     * @param frameCount Number of stereo frames in the block
     */
    fun processBlock(
        inputStereo: FloatArray,
        outputStereo: FloatArray,
        frameCount: Int
    ) {
        if (transportState == TransportState.STOPPED && !isTapeDecelerating) return

        val hasSolo = tracks.any { it.isSolo }
        val loopLen = if (isLoopEnabled) max(1, loopEndFrame - loopStartFrame) else totalFrames

        for (f in 0 until frameCount) {
            // Smooth tape motor inertia deceleration
            if (isTapeDecelerating) {
                currentSpeed *= 0.9992f
                if (Math.abs(currentSpeed) < 0.02f) {
                    currentSpeed = 0.0f
                    isTapeDecelerating = false
                    transportState = TransportState.PAUSED
                }
            }

            val inIdx = f * 2
            val inLeft = inputStereo[inIdx]
            val inRight = inputStereo[inIdx + 1]

            val currentFrameInt = floor(playheadFrame).toInt()

            // 1. Sound-on-sound overdubbing for armed tracks
            if (isRecording && Math.abs(currentSpeed) > 0.01f) {
                for (t in 0..3) {
                    val track = tracks[t]
                    if (track.isArmedForRecording) {
                        track.overdubFrame(currentFrameInt, inLeft, inRight)
                    }
                }
            }

            // 2. Playback summing from all 4 tracks
            var tapeOutLeft = 0.0f
            var tapeOutRight = 0.0f

            for (t in 0..3) {
                val track = tracks[t]
                if (hasSolo) {
                    if (!track.isSolo) continue
                } else {
                    if (track.isMuted) continue
                }

                val (tl, tr) = track.readFrame(playheadFrame, loopLen)
                tapeOutLeft += tl
                tapeOutRight += tr
            }

            // Mix tape playback into the master output buffer
            val outIdx = f * 2
            outputStereo[outIdx] += tapeOutLeft
            outputStereo[outIdx + 1] += tapeOutRight

            // Advance playhead by current variable playback speed
            playheadFrame += currentSpeed

            // Loop wrapping logic
            if (isLoopEnabled) {
                if (currentSpeed >= 0.0f) {
                    if (playheadFrame >= loopEndFrame) {
                        playheadFrame = loopStartFrame.toDouble() + (playheadFrame - loopEndFrame)
                    }
                } else {
                    if (playheadFrame < loopStartFrame) {
                        playheadFrame = loopEndFrame.toDouble() - (loopStartFrame - playheadFrame)
                    }
                }
            } else {
                if (playheadFrame >= totalFrames) {
                    playheadFrame = totalFrames - 1.0
                    transportState = TransportState.STOPPED
                } else if (playheadFrame < 0.0) {
                    playheadFrame = 0.0
                    transportState = TransportState.STOPPED
                }
            }
        }
    }
}
