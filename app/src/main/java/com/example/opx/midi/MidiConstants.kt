package com.example.opx.midi

/**
 * Android MIDI API constants and default mappings for OP-X.
 * Conforms to Section 5.1 of the OP-X Architectural Specification.
 */
object MidiConstants {
    const val CC_CUTOFF_BLUE = 74      // Blue Touch Encoder -> Filter Cutoff
    const val CC_RESONANCE_OCHRE = 71  // Ochre Touch Encoder -> Resonance
    const val CC_FX_SLATE = 16         // Slate Touch Encoder -> General CC 1 / FX Send
    const val CC_MOD_ORANGE = 17       // Orange Touch Encoder -> General CC 2 / Modulation

    const val CC_VOLUME = 7
    const val CC_PAN = 10
    const val CC_ALL_NOTES_OFF = 123

    const val DEFAULT_CHANNEL = 0 // Channel 1 (0-indexed)
}
