package com.example.opx.sequencer

import com.example.opx.audio.engine.SynthEngine
import com.example.opx.audio.engine.SynthParameters
import kotlin.random.Random

/**
 * 4-Track Polymetric 16-Step Sequencer and Generative Tombola Engine.
 *
 * Drives sample-accurate rhythmic step playback directly from the audio rendering clock.
 * Supports independent track lengths (polymetric polyrhythms), per-step parameter locks,
 * and generative Euclidean/Tombola triggers.
 */
class StepSequencer(
    val sampleRate: Int = 48000,
    initialBpm: Float = 120.0f
) {
    val tracks = Array(4) { id ->
        val defaultLengths = intArrayOf(16, 7, 5, 12) // Polyrhythmic lengths out of the box!
        SequencerTrack(
            trackId = id,
            initialLength = defaultLengths[id % defaultLengths.size],
            defaultBaseNote = when (id) {
                0 -> 48 // C3 (Bassline)
                1 -> 60 // C4 (Lead)
                2 -> 67 // G4 (Arp)
                else -> 72 // C5 (Chime/Perc)
            }
        )
    }

    val tombola = TombolaPhysics()

    var bpm: Float = initialBpm
        set(value) { field = value.coerceIn(40.0f, 260.0f) }

    var swing: Float = 0.0f // 0.0 = straight, 0.5 = heavy triplet swing
        set(value) { field = value.coerceIn(0.0f, 0.75f) }

    var isPlaying: Boolean = false
    var isTombolaMode: Boolean = false

    // Audio-rate clock accumulator
    private var clockFrameAccumulator: Double = 0.0
    private var current16thStepGlobal: Long = 0L

    // Active active note-off events with countdown in frames
    private val pendingNoteOffs = mutableListOf<PendingNoteOff>()

    private data class PendingNoteOff(
        var remainingFrames: Int,
        val midiNote: Int
    )

    fun play() {
        isPlaying = true
        tombola.isRunning = true
    }

    fun stop() {
        isPlaying = false
        tombola.isRunning = false
        clockFrameAccumulator = 0.0
        current16thStepGlobal = 0L
        tracks.forEach { it.reset() }
        pendingNoteOffs.clear()
    }

    fun togglePlay() {
        if (isPlaying) stop() else play()
    }

    /**
     * Processed synchronously by AudioEngine at each audio frame buffer.
     * Guarantees sample-accurate sequencing jitter-free timing.
     */
    fun processClock(frameCount: Int, synthEngine: SynthEngine) {
        // Handle pending note-off releases
        val iter = pendingNoteOffs.iterator()
        while (iter.hasNext()) {
            val pending = iter.next()
            pending.remainingFrames -= frameCount
            if (pending.remainingFrames <= 0) {
                synthEngine.noteOff(pending.midiNote)
                iter.remove()
            }
        }

        if (!isPlaying) return

        if (isTombolaMode) {
            // Step Tombola physics simulation
            val events = tombola.update()
            for (ev in events) {
                synthEngine.noteOn(ev.midiNote, ev.velocity)
                pendingNoteOffs.add(
                    PendingNoteOff(
                        remainingFrames = (sampleRate * 0.12f).toInt(),
                        midiNote = ev.midiNote
                    )
                )
            }
            return
        }

        // Standard 16-step clock calculation (16th note = 1/4 of a quarter note beat)
        val framesPerQuarterNote = (sampleRate * 60.0) / bpm
        val baseFramesPer16th = framesPerQuarterNote / 4.0

        clockFrameAccumulator += frameCount

        // Check if step boundary crossed
        val isEvenStep = (current16thStepGlobal % 2L) == 0L
        val effectiveStepFrames = if (isEvenStep) {
            baseFramesPer16th * (1.0 + swing * 0.5)
        } else {
            baseFramesPer16th * (1.0 - swing * 0.5)
        }

        if (clockFrameAccumulator >= effectiveStepFrames) {
            clockFrameAccumulator -= effectiveStepFrames
            current16thStepGlobal++

            // Advance all 4 independent tracks (polymetric lengths)
            for (t in 0..3) {
                val track = tracks[t]
                if (track.isMuted) continue

                val step = track.advance()
                if (step.isActive) {
                    // Check probabilistic trigger
                    val triggerPass = if (step.probability >= 1.0f) true else Random.nextFloat() <= step.probability
                    if (triggerPass) {
                        // Apply per-step parameter locks if present
                        if (step.hasParameterLocks) {
                            applyParameterLocks(step, synthEngine)
                        }

                        // Trigger synth note
                        synthEngine.noteOn(step.midiNote, step.velocity)

                        // Register note-off based on gate duration
                        val gateFrames = (effectiveStepFrames * step.gateRatio.coerceIn(0.1f, 0.95f)).toInt()
                        pendingNoteOffs.add(
                            PendingNoteOff(
                                remainingFrames = gateFrames,
                                midiNote = step.midiNote
                            )
                        )
                    }
                }
            }
        }
    }

    private fun applyParameterLocks(step: StepData, synthEngine: SynthEngine) {
        val currentParams = synthEngine.params
        val lockedCutoff = step.lockedCutoffHz ?: currentParams.cutoffHz
        val lockedRes = step.lockedResonance ?: currentParams.resonance
        val lockedFx = step.lockedFxSend ?: currentParams.fxSend
        val lockedMod = step.lockedModulation ?: currentParams.modulation

        synthEngine.params = currentParams.copy(
            cutoffHz = lockedCutoff,
            resonance = lockedRes,
            fxSend = lockedFx,
            modulation = lockedMod
        )
    }

    /**
     * Initializes a default melodic groove across the tracks for instant jamming.
     */
    fun loadFactoryGroove() {
        tracks[0].generateEuclidean(pulses = 4, totalSteps = 16) // Solid 4-on-the-floor bass
        tracks[1].generateEuclidean(pulses = 5, totalSteps = 7)  // 5 over 7 polymetric lead
        tracks[2].generateEuclidean(pulses = 3, totalSteps = 5)  // 3 over 5 polyrhythmic arpeggio
        tracks[3].generateEuclidean(pulses = 7, totalSteps = 12) // 7 over 12 syncopated accents

        // Add some parameter locks
        tracks[1].steps[0].lockedCutoffHz = 9500f
        tracks[1].steps[0].lockedFxSend = 0.6f
        tracks[2].steps[2].lockedCutoffHz = 7200f
    }
}
