package com.example.opx.audio.engine

import com.example.opx.audio.dsp.Voice
import com.example.opx.audio.dsp.Waveform
import com.example.opx.model.Note
import kotlin.math.tanh

/**
 * Polyphonic Synthesizer Engine managing voice allocation,
 * parameter dispatch, and stereo audio rendering.
 */
class SynthEngine(
    val maxVoices: Int = 8,
    var sampleRate: Int = 48000
) {
    private val voices = Array(maxVoices) { id -> Voice(id, sampleRate) }

    // Lock-free visualizer bridge
    val visualizerBridge = AudioVisualizerBridge(512)

    // Current parameters (volatile for thread safety)
    @Volatile
    var params: SynthParameters = SynthParameters()
        set(value) {
            field = value
            applyParamsToVoices(value)
        }

    init {
        applyParamsToVoices(params)
    }

    fun updateSampleRate(newSampleRate: Int) {
        sampleRate = newSampleRate
        voices.forEach { it.updateSampleRate(newSampleRate) }
        applyParamsToVoices(params)
    }

    private fun applyParamsToVoices(p: SynthParameters) {
        val driveFactor = 1.0f + p.fxSend * 3.0f // FX Send drives the filter saturation
        val pwm = 0.5f + (p.modulation - 0.5f) * 0.8f // Orange encoder modulates pulse-width
        val detune = p.detuneCents + p.modulation * 15.0f

        voices.forEach { voice ->
            voice.osc1.waveform = p.waveform
            voice.osc2.waveform = p.waveform
            voice.osc1.pulseWidth = pwm
            voice.osc2.pulseWidth = pwm
            voice.osc1.subLevel = p.subLevel
            voice.detuneCents = detune

            voice.filter.resonance = p.resonance
            voice.filter.drive = driveFactor

            voice.ampEnvelope.attackTimeSec = p.attackSec
            voice.ampEnvelope.decayTimeSec = p.decaySec
            voice.ampEnvelope.sustainLevel = p.sustainLevel
            voice.ampEnvelope.releaseTimeSec = p.releaseSec
        }
    }

    /**
     * Triggers a musical note on an available or stolen voice.
     */
    fun noteOn(midiNote: Int, velocity: Float = 0.9f) {
        val effectiveNote = midiNote + params.octaveOffset * 12
        val freq = Note.midiToFrequency(effectiveNote)

        // 1. Check if the note is already ringing on a voice; retrigger if so
        val existingVoice = voices.find { it.isActive && it.noteNumber == effectiveNote }
        if (existingVoice != null) {
            existingVoice.noteOn(effectiveNote, velocity, freq)
            return
        }

        // 2. Find an idle voice
        val idleVoice = voices.find { !it.isActive }
        if (idleVoice != null) {
            idleVoice.noteOn(effectiveNote, velocity, freq)
            return
        }

        // 3. Voice stealing: steal the oldest active voice
        val oldestVoice = voices.minByOrNull { it.triggerTimestamp } ?: voices[0]
        oldestVoice.noteOn(effectiveNote, velocity, freq)
    }

    /**
     * Releases a note (initiates release envelope).
     */
    fun noteOff(midiNote: Int) {
        val effectiveNote = midiNote + params.octaveOffset * 12
        voices.filter { it.noteNumber == effectiveNote }.forEach { it.noteOff() }
    }

    /**
     * Releases all sounding voices immediately.
     */
    fun allNotesOff() {
        voices.forEach { it.stop() }
    }

    val activeVoiceCount: Int
        get() = voices.count { it.isActive }

    /**
     * Renders a block of interleaved stereo float samples into the output buffer.
     * Interleaved layout: [Left0, Right0, Left1, Right1, ...]
     * @param output Interleaved stereo float array.
     * @param frameCount Number of stereo frames to render.
     */
    fun renderBlock(output: FloatArray, frameCount: Int) {
        val baseCutoff = params.cutoffHz
        val masterVol = params.masterVolume

        var outIdx = 0
        for (f in 0 until frameCount) {
            var sumLeft = 0.0f
            var sumRight = 0.0f

            for (v in 0 until maxVoices) {
                val voice = voices[v]
                if (voice.isActive) {
                    val (l, r) = voice.process(baseCutoff, oscMix = 0.6f)
                    sumLeft += l
                    sumRight += r
                }
            }

            // Master soft saturation / limiter using tanh to prevent harsh clipping
            val masterLeft = tanh(sumLeft * masterVol)
            val masterRight = tanh(sumRight * masterVol)

            output[outIdx++] = masterLeft
            output[outIdx++] = masterRight

            // Feed visualizer bridge with mono mix of current sample
            visualizerBridge.writeSample((masterLeft + masterRight) * 0.5f)
        }
    }
}
