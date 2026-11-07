package com.example.opx.sequencer

import kotlin.random.Random

/**
 * Single polymetric sequencer track.
 * Supports independent track length (1 to 16 steps) for creating complex polyrhythms
 * that phase-shift across measure bars.
 */
class SequencerTrack(
    val trackId: Int,
    initialLength: Int = 16,
    val defaultBaseNote: Int = 48 + (trackId * 7) // Varied root notes per track
) {
    // Independent length for polymetric polyrhythms (e.g. 5, 7, 11, 16)
    var length: Int = initialLength.coerceIn(1, 16)
        set(value) {
            field = value.coerceIn(1, 16)
            if (currentStepIndex >= field) {
                currentStepIndex = 0
            }
        }

    val steps: Array<StepData> = Array(16) { i ->
        StepData(stepIndex = i, midiNote = defaultBaseNote)
    }

    var currentStepIndex: Int = 0
    var isMuted: Boolean = false
    var octaveOffset: Int = 0

    fun reset() {
        currentStepIndex = 0
    }

    fun clear() {
        steps.forEach {
            it.isActive = false
            it.clearLocks()
        }
    }

    /**
     * Advances to the next step based on independent track length.
     * Returns the step to be triggered.
     */
    fun advance(): StepData {
        val step = steps[currentStepIndex]
        currentStepIndex = (currentStepIndex + 1) % length
        return step
    }

    /**
     * Generates a Euclidean rhythm distribution (Bjorklund algorithm).
     * Distributes [pulses] active beats as evenly as possible over [totalSteps].
     */
    fun generateEuclidean(pulses: Int, totalSteps: Int = length) {
        val k = pulses.coerceIn(1, totalSteps)
        val n = totalSteps.coerceIn(1, 16)
        length = n

        clear()

        // Bresenham-style euclidean spacing
        for (i in 0 until n) {
            val isActive = ((i * k) % n) < k
            steps[i].isActive = isActive
            steps[i].midiNote = defaultBaseNote + (octaveOffset * 12)
        }
    }

    /**
     * Randomizes active steps and notes from a musical scale.
     */
    fun randomize(density: Float = 0.5f, scaleNotes: List<Int> = listOf(0, 3, 5, 7, 10)) {
        val rng = Random(System.nanoTime() + trackId * 1000)
        for (i in 0 until length) {
            val active = rng.nextFloat() < density
            steps[i].isActive = active
            if (active) {
                val scaleDegree = scaleNotes[rng.nextInt(scaleNotes.size)]
                steps[i].midiNote = defaultBaseNote + scaleDegree + (octaveOffset * 12)
                steps[i].velocity = 0.6f + (rng.nextFloat() * 0.4f)
                
                // 30% chance of random parameter lock
                if (rng.nextFloat() < 0.35f) {
                    steps[i].lockedCutoffHz = 400f + (rng.nextFloat() * 8000f)
                }
                if (rng.nextFloat() < 0.25f) {
                    steps[i].lockedFxSend = rng.nextFloat() * 0.7f
                }
            }
        }
    }
}
