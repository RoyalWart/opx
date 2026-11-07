package com.example.opx.audio.dsp

import kotlin.math.pow

/**
 * Represents a single polyphonic synth voice.
 * Combines dual oscillators (for detune/unison cluster), sub-oscillator,
 * resonant filter, and dedicated ADSR envelope generators.
 */
class Voice(
    val voiceId: Int,
    private var sampleRate: Int = 48000
) {
    var noteNumber: Int = -1
    var velocity: Float = 0.0f
    var triggerTimestamp: Long = 0L

    val osc1 = Oscillator(Waveform.SAW, sampleRate)
    val osc2 = Oscillator(Waveform.SAW, sampleRate) // Detuned cluster oscillator
    val filter = MoogFilter(sampleRate)
    val ampEnvelope = AdsrEnvelope(sampleRate)
    val filterEnvelope = AdsrEnvelope(sampleRate)

    var detuneCents: Float = 5.0f // Unison detune amount in cents
    var filterEnvAmount: Float = 2000.0f // Filter cutoff modulation depth in Hz
    var pan: Float = 0.5f // 0.0 = Left, 1.0 = Right

    val isActive: Boolean
        get() = ampEnvelope.isActive

    fun updateSampleRate(newSampleRate: Int) {
        sampleRate = newSampleRate
        osc1.sampleRate = newSampleRate
        osc2.sampleRate = newSampleRate
        filter.updateSampleRate(newSampleRate)
        ampEnvelope.updateSampleRate(newSampleRate)
        filterEnvelope.updateSampleRate(newSampleRate)
    }

    fun noteOn(midiNote: Int, vel: Float, freqHz: Float) {
        noteNumber = midiNote
        velocity = vel
        triggerTimestamp = System.nanoTime()

        // Configure oscillator frequencies with unison detune
        osc1.setFrequency(freqHz)
        val detuneRatio = 2.0.pow((detuneCents / 1200.0).toDouble()).toFloat()
        osc2.setFrequency(freqHz * detuneRatio)

        osc1.resetPhase()
        osc2.resetPhase()

        ampEnvelope.noteOn()
        filterEnvelope.noteOn()
    }

    fun noteOff() {
        ampEnvelope.noteOff()
        filterEnvelope.noteOff()
    }

    fun stop() {
        ampEnvelope.reset()
        filterEnvelope.reset()
        filter.reset()
        noteNumber = -1
    }

    /**
     * Synthesizes one stereo audio frame.
     * @param baseCutoff Base filter cutoff frequency in Hz.
     * @param oscMix Mix balance between osc1 and detuned osc2.
     * @return Pair of Left and Right audio sample amplitudes.
     */
    fun process(baseCutoff: Float, oscMix: Float = 0.5f): Pair<Float, Float> {
        if (!isActive) return Pair(0.0f, 0.0f)

        // Generate oscillator signals
        val s1 = osc1.nextSample()
        val s2 = osc2.nextSample()
        val mixedOsc = s1 * (1.0f - oscMix * 0.5f) + s2 * (oscMix * 0.5f)

        // Dynamic filter cutoff modulation by filter envelope
        val filterEnvVal = filterEnvelope.process()
        val dynamicCutoff = (baseCutoff + filterEnvVal * filterEnvAmount).coerceIn(20.0f, sampleRate * 0.45f)
        filter.cutoffHz = dynamicCutoff

        // Filter the signal
        val filtered = filter.process(mixedOsc)

        // Apply amplitude envelope and velocity
        val ampVal = ampEnvelope.process()
        val finalSample = filtered * ampVal * velocity

        // Stereo panning
        val left = finalSample * (1.0f - pan)
        val right = finalSample * pan

        return Pair(left, right)
    }
}
