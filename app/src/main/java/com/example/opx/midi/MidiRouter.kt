package com.example.opx.midi

import android.content.Context
import com.example.opx.audio.engine.SynthEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MidiLogEntry(
    val timestamp: String,
    val hex: String,
    val description: String,
    val isOutbound: Boolean
)

/**
 * Centralized MIDI routing bridge connecting Touch UI, Color Encoders,
 * Step Sequencer, USB-MIDI hardware, and Wi-Fi Network MIDI broadcasts.
 */
class MidiRouter(
    context: Context,
    val synthEngine: SynthEngine
) {
    var defaultChannel: Int = 0 // Channel 1 (0-15)

    var isUsbEnabled: Boolean = true
    var isNetworkEnabled: Boolean = true

    val networkBroadcaster = NetworkMidiBroadcaster()
    val usbMidiManager = AndroidUsbMidiManager(context) { incomingBytes ->
        handleIncomingMidi(incomingBytes)
    }

    // Circular activity monitor log (last 40 entries)
    private val _activityLog = mutableListOf<MidiLogEntry>()
    val activityLog: List<MidiLogEntry>
        get() = synchronized(_activityLog) { _activityLog.toList() }

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun sendNoteOn(note: Int, velocity: Float, channel: Int = defaultChannel) {
        val velInt = (velocity * 127).toInt().coerceIn(1, 127)
        val packet = MidiPacket.noteOn(channel, note, velInt)
        dispatchOutbound(packet)
    }

    fun sendNoteOff(note: Int, channel: Int = defaultChannel) {
        val packet = MidiPacket.noteOff(channel, note, 0)
        dispatchOutbound(packet)
    }

    fun sendControlChange(controller: Int, valueNormalized: Float, channel: Int = defaultChannel) {
        val valInt = (valueNormalized * 127).toInt().coerceIn(0, 127)
        val packet = MidiPacket.controlChange(channel, controller, valInt)
        dispatchOutbound(packet)
    }

    fun sendPitchBend(bendNormalized: Float, channel: Int = defaultChannel) {
        // bendNormalized: -1.0 to +1.0
        val bend14Bit = (bendNormalized * 8191).toInt()
        val packet = MidiPacket.pitchBend(channel, bend14Bit)
        dispatchOutbound(packet)
    }

    fun sendTransport(isStart: Boolean) {
        val packet = if (isStart) MidiPacket.transportStart() else MidiPacket.transportStop()
        dispatchOutbound(packet)
    }

    fun panicAllNotesOff() {
        for (ch in 0..15) {
            val packet = MidiPacket.allNotesOff(ch)
            dispatchOutbound(packet)
        }
        synthEngine.allNotesOff()
    }

    private fun dispatchOutbound(packet: ByteArray) {
        if (isUsbEnabled) {
            usbMidiManager.send(packet)
        }
        if (isNetworkEnabled) {
            networkBroadcaster.send(packet)
        }
        logEvent(packet, isOutbound = true)
    }

    private fun handleIncomingMidi(bytes: ByteArray) {
        logEvent(bytes, isOutbound = false)

        if (bytes.isEmpty()) return
        val status = bytes[0].toInt() and 0xFF
        val type = status and 0xF0

        when (type) {
            0x90 -> { // Note On
                if (bytes.size >= 3) {
                    val note = bytes[1].toInt() and 0x7F
                    val vel = bytes[2].toInt() and 0x7F
                    if (vel > 0) {
                        synthEngine.noteOn(note, vel / 127.0f)
                    } else {
                        synthEngine.noteOff(note)
                    }
                }
            }
            0x80 -> { // Note Off
                if (bytes.size >= 2) {
                    val note = bytes[1].toInt() and 0x7F
                    synthEngine.noteOff(note)
                }
            }
            0xB0 -> { // CC
                if (bytes.size >= 3) {
                    val cc = bytes[1].toInt() and 0x7F
                    val value = (bytes[2].toInt() and 0x7F) / 127.0f
                    handleMidiCc(cc, value)
                }
            }
        }
    }

    private fun handleMidiCc(cc: Int, value: Float) {
        val p = synthEngine.params
        when (cc) {
            MidiConstants.CC_CUTOFF_BLUE -> {
                val hz = 20f + (17980f * value * value)
                synthEngine.params = p.copy(cutoffHz = hz)
            }
            MidiConstants.CC_RESONANCE_OCHRE -> {
                synthEngine.params = p.copy(resonance = value * 0.95f)
            }
            MidiConstants.CC_FX_SLATE -> {
                synthEngine.params = p.copy(fxSend = value)
            }
            MidiConstants.CC_MOD_ORANGE -> {
                synthEngine.params = p.copy(modulation = value)
            }
            MidiConstants.CC_ALL_NOTES_OFF -> {
                synthEngine.allNotesOff()
            }
        }
    }

    private fun logEvent(bytes: ByteArray, isOutbound: Boolean) {
        val desc = MidiPacket.formatDescription(bytes)
        val hex = bytes.joinToString(" ") { "%02X".format(it) }
        val now = timeFormat.format(Date())

        synchronized(_activityLog) {
            _activityLog.add(0, MidiLogEntry(now, hex, desc, isOutbound))
            if (_activityLog.size > 40) {
                _activityLog.removeAt(_activityLog.size - 1)
            }
        }
    }

    fun release() {
        usbMidiManager.release()
        networkBroadcaster.release()
    }
}
