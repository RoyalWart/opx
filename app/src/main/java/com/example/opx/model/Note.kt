package com.example.opx.model

import kotlin.math.pow

/**
 * Musical note utilities for MIDI note conversion, frequencies, and note names.
 */
object Note {
    private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    /**
     * Converts a MIDI note number (0 - 127) to frequency in Hz (A4 = 440 Hz).
     */
    fun midiToFrequency(midiNote: Int): Float {
        return (440.0 * 2.0.pow((midiNote - 69) / 12.0)).toFloat()
    }

    /**
     * Gets the standard musical name with octave, e.g. "C4", "F#3".
     */
    fun getNoteName(midiNote: Int): String {
        val noteIndex = (midiNote % 12).let { if (it < 0) it + 12 else it }
        val octave = (midiNote / 12) - 1
        return "${NOTE_NAMES[noteIndex]}$octave"
    }

    /**
     * Returns true if note is a black key (sharp/flat).
     */
    fun isBlackKey(midiNote: Int): Boolean {
        val noteInOctave = (midiNote % 12).let { if (it < 0) it + 12 else it }
        return noteInOctave in intArrayOf(1, 3, 6, 8, 10)
    }
}
