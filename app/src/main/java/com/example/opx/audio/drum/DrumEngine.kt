package com.example.opx.audio.drum

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class DrumType(val displayName: String, val defaultPitch: Float) {
    KICK("808 KICK", 55.0f),
    SNARE("ANALOG SNARE", 185.0f),
    CLOSED_HAT("CLOSED HAT", 8000.0f),
    OPEN_HAT("OPEN HAT", 7500.0f),
    CLAP("MULTI CLAP", 1200.0f),
    TOM_LOW("LOW TOM", 95.0f),
    TOM_HIGH("HIGH TOM", 155.0f),
    CRASH("CRASH / PERC", 5500.0f)
}

data class DrumVoice(
    val id: Int,
    val type: DrumType,
    var pitch: Float = type.defaultPitch,
    var decaySec: Float = 0.25f,
    var level: Float = 0.90f,
    var pan: Float = 0.5f,
    var isTriggered: Boolean = false,
    var sampleFrameIndex: Int = 0,
    var velocity: Float = 0.9f
)

/**
 * 8-Pad RAM Drum Sampler & Analog Transient Synthesis Engine.
 * Generates punchy kicks, snappy snares, metallic hats, claps, and toms in real time.
 */
class DrumEngine(
    private var sampleRate: Int = 48000
) {
    val voices = Array(8) { id ->
        val type = DrumType.values()[id % DrumType.values().size]
        DrumVoice(
            id = id,
            type = type,
            pitch = type.defaultPitch,
            decaySec = when (type) {
                DrumType.KICK -> 0.35f
                DrumType.SNARE -> 0.22f
                DrumType.CLOSED_HAT -> 0.06f
                DrumType.OPEN_HAT -> 0.45f
                DrumType.CLAP -> 0.20f
                DrumType.TOM_LOW, DrumType.TOM_HIGH -> 0.28f
                DrumType.CRASH -> 0.85f
            }
        )
    }

    private val rng = Random(System.nanoTime())

    fun updateSampleRate(newSampleRate: Int) {
        sampleRate = newSampleRate
    }

    fun triggerPad(padIndex: Int, velocity: Float = 0.95f) {
        if (padIndex in 0 until voices.size) {
            val v = voices[padIndex]
            v.isTriggered = true
            v.sampleFrameIndex = 0
            v.velocity = velocity.coerceIn(0.1f, 1.0f)
        }
    }

    /**
     * Renders active drum voices and mixes into the stereo output block.
     */
    fun processBlock(outputBuffer: FloatArray, frameCount: Int) {
        for (v in voices) {
            if (!v.isTriggered) continue

            val totalFrames = (v.decaySec * sampleRate).toInt().coerceAtLeast(100)
            val pitchBase = v.pitch

            for (f in 0 until frameCount) {
                val frame = v.sampleFrameIndex + f
                if (frame >= totalFrames) {
                    v.isTriggered = false
                    break
                }

                val t = frame.toFloat() / sampleRate.toFloat()
                val env = exp((-t * 4.5f / v.decaySec).toDouble()).toFloat()

                // Synthesize specific drum voice transient
                val sampleMono = when (v.type) {
                    DrumType.KICK -> {
                        // Exponential pitch drop transient: from 160Hz down to sub bass 45Hz
                        val dynamicPitch = pitchBase + 120.0f * exp((-t * 35.0f).toDouble()).toFloat()
                        val phase = 2.0f * PI.toFloat() * dynamicPitch * t
                        val sub = sin(phase)
                        // Add slight punch click at the start
                        val click = if (t < 0.005f) (1.0f - t / 0.005f) * 0.4f else 0.0f
                        (sub + click) * env
                    }
                    DrumType.SNARE -> {
                        // Body tone + white noise snare wire
                        val body = sin(2.0f * PI.toFloat() * pitchBase * t) * 0.5f
                        val noise = (rng.nextFloat() * 2.0f - 1.0f) * 0.7f
                        (body + noise) * env
                    }
                    DrumType.CLOSED_HAT -> {
                        // Metallic high-frequency inharmonic cluster
                        val tone1 = sin(2.0f * PI.toFloat() * pitchBase * t)
                        val tone2 = sin(2.0f * PI.toFloat() * (pitchBase * 1.48f) * t)
                        val noise = (rng.nextFloat() * 2.0f - 1.0f) * 0.6f
                        (tone1 * 0.3f + tone2 * 0.3f + noise) * env
                    }
                    DrumType.OPEN_HAT -> {
                        val tone1 = sin(2.0f * PI.toFloat() * pitchBase * t)
                        val noise = (rng.nextFloat() * 2.0f - 1.0f) * 0.8f
                        (tone1 * 0.2f + noise) * env
                    }
                    DrumType.CLAP -> {
                        // Multi-burst transient
                        val burstEnv = when {
                            t < 0.012f -> sin(t * 100f)
                            t < 0.024f -> sin((t - 0.012f) * 100f) * 0.8f
                            else -> exp((-(t - 0.024f) * 12.0f).toDouble()).toFloat()
                        }
                        val noise = rng.nextFloat() * 2.0f - 1.0f
                        noise * burstEnv
                    }
                    DrumType.TOM_LOW, DrumType.TOM_HIGH -> {
                        val pitchDrop = pitchBase + 40.0f * exp((-t * 18.0f).toDouble()).toFloat()
                        sin(2.0f * PI.toFloat() * pitchDrop * t) * env
                    }
                    DrumType.CRASH -> {
                        val noise = rng.nextFloat() * 2.0f - 1.0f
                        noise * env
                    }
                }

                val finalAmp = sampleMono * v.level * v.velocity
                val outIdx = f * 2
                outputBuffer[outIdx] += finalAmp * (1.0f - v.pan)
                outputBuffer[outIdx + 1] += finalAmp * v.pan
            }

            v.sampleFrameIndex += frameCount
        }
    }
}
