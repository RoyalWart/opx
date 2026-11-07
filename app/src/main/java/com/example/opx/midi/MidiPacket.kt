package com.example.opx.midi

import com.example.opx.model.Note

/**
 * Encodes standard raw MIDI 1.0 byte messages for USB and Network transmission.
 */
object MidiPacket {
    fun noteOn(channel: Int, note: Int, velocity: Int): ByteArray {
        val status = 0x90 or (channel.coerceIn(0, 15))
        return byteArrayOf(
            status.toByte(),
            note.coerceIn(0, 127).toByte(),
            velocity.coerceIn(0, 127).toByte()
        )
    }

    fun noteOff(channel: Int, note: Int, velocity: Int = 0): ByteArray {
        val status = 0x80 or (channel.coerceIn(0, 15))
        return byteArrayOf(
            status.toByte(),
            note.coerceIn(0, 127).toByte(),
            velocity.coerceIn(0, 127).toByte()
        )
    }

    fun controlChange(channel: Int, controller: Int, value: Int): ByteArray {
        val status = 0xB0 or (channel.coerceIn(0, 15))
        return byteArrayOf(
            status.toByte(),
            controller.coerceIn(0, 127).toByte(),
            value.coerceIn(0, 127).toByte()
        )
    }

    /**
     * Encodes 14-bit pitch bend (-8192 to +8191, center = 0).
     */
    fun pitchBend(channel: Int, bendValue14Bit: Int): ByteArray {
        val status = 0xE0 or (channel.coerceIn(0, 15))
        val normalized = (bendValue14Bit + 8192).coerceIn(0, 16383)
        val lsb = normalized and 0x7F
        val msb = (normalized shr 7) and 0x7F
        return byteArrayOf(status.toByte(), lsb.toByte(), msb.toByte())
    }

    fun allNotesOff(channel: Int): ByteArray {
        return controlChange(channel, MidiConstants.CC_ALL_NOTES_OFF, 0)
    }

    fun transportStart(): ByteArray = byteArrayOf(0xFA.toByte())
    fun transportStop(): ByteArray = byteArrayOf(0xFC.toByte())
    fun transportContinue(): ByteArray = byteArrayOf(0xFB.toByte())
    fun transportClock(): ByteArray = byteArrayOf(0xF8.toByte())

    /**
     * Formats raw MIDI bytes into human-readable debug logs.
     */
    fun formatDescription(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        val status = bytes[0].toInt() and 0xFF
        val type = status and 0xF0
        val ch = (status and 0x0F) + 1

        return when (type) {
            0x90 -> {
                if (bytes.size >= 3 && bytes[2] > 0) {
                    val note = bytes[1].toInt() and 0x7F
                    val vel = bytes[2].toInt() and 0x7F
                    "NOTE ON  Ch:$ch ${Note.getNoteName(note)} ($note) Vel:$vel"
                } else {
                    val note = if (bytes.size >= 2) bytes[1].toInt() and 0x7F else 0
                    "NOTE OFF Ch:$ch ${Note.getNoteName(note)} ($note)"
                }
            }
            0x80 -> {
                val note = if (bytes.size >= 2) bytes[1].toInt() and 0x7F else 0
                "NOTE OFF Ch:$ch ${Note.getNoteName(note)} ($note)"
            }
            0xB0 -> {
                val cc = if (bytes.size >= 2) bytes[1].toInt() and 0x7F else 0
                val value = if (bytes.size >= 3) bytes[2].toInt() and 0x7F else 0
                val ccName = when (cc) {
                    MidiConstants.CC_CUTOFF_BLUE -> "CUTOFF (Blue)"
                    MidiConstants.CC_RESONANCE_OCHRE -> "RESONANCE (Ochre)"
                    MidiConstants.CC_FX_SLATE -> "FX SEND 1 (Slate)"
                    MidiConstants.CC_MOD_ORANGE -> "MODULATION (Orange)"
                    MidiConstants.CC_ALL_NOTES_OFF -> "ALL NOTES OFF"
                    else -> "CC $cc"
                }
                "CC $cc ($ccName) Ch:$ch Val:$value"
            }
            0xE0 -> {
                val lsb = if (bytes.size >= 2) bytes[1].toInt() and 0x7F else 0
                val msb = if (bytes.size >= 3) bytes[2].toInt() and 0x7F else 0
                val total = (msb shl 7) or lsb
                "PITCH BEND Ch:$ch Val:${total - 8192}"
            }
            0xF0 -> {
                when (status) {
                    0xFA -> "TRANSPORT START"
                    0xFC -> "TRANSPORT STOP"
                    0xFB -> "TRANSPORT CONTINUE"
                    0xF8 -> "TIMING CLOCK"
                    else -> "SYSTEM REALTIME (0x${Integer.toHexString(status)})"
                }
            }
            else -> "RAW [${bytes.joinToString(" ") { "%02X".format(it) }}]"
        }
    }
}
