package com.example.opx.midi

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Manages standard Android USB-MIDI class-compliant device discovery,
 * input/output port connections, and hardware hotplug callbacks.
 */
class AndroidUsbMidiManager(
    context: Context,
    private val onMidiReceived: (ByteArray) -> Unit
) {
    companion object {
        private const val TAG = "OPX_UsbMidi"
    }

    private val midiManager: MidiManager? = context.getSystemService(Context.MIDI_SERVICE) as? MidiManager
    private val mainHandler = Handler(Looper.getMainLooper())

    // Active connected USB devices & ports
    private val openDevices = mutableListOf<MidiDevice>()
    private val activeInputPorts = mutableListOf<MidiInputPort>()

    @Volatile
    var connectedDeviceNames: List<String> = emptyList()
        private set

    @Volatile
    var isUsbMidiSupported: Boolean = (midiManager != null)
        private set

    private val deviceCallback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) {
            Log.i(TAG, "USB-MIDI device attached: ${getDeviceName(device)}")
            scanAndConnectDevices()
        }

        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            Log.i(TAG, "USB-MIDI device removed: ${getDeviceName(device)}")
            scanAndConnectDevices()
        }
    }

    init {
        try {
            midiManager?.registerDeviceCallback(deviceCallback, mainHandler)
            scanAndConnectDevices()
        } catch (e: Exception) {
            Log.w(TAG, "Could not initialize USB MIDI manager", e)
        }
    }

    /**
     * Scans all connected USB-MIDI devices and establishes ports.
     */
    fun scanAndConnectDevices() {
        val mgr = midiManager ?: return

        try {
            closeAllPorts()
            val devices = mgr.devices
            val names = mutableListOf<String>()

            for (info in devices) {
                val devName = getDeviceName(info)
                names.add(devName)

                // Open device
                mgr.openDevice(info, { device ->
                    if (device != null) {
                        synchronized(openDevices) {
                            openDevices.add(device)
                        }

                        // 1. Open Output ports on OP-X to send MIDI to USB device
                        info.ports.filter { it.type == MidiDeviceInfo.PortInfo.TYPE_INPUT }.forEach { port ->
                            val inputPort = device.openInputPort(port.portNumber)
                            if (inputPort != null) {
                                synchronized(activeInputPorts) {
                                    activeInputPorts.add(inputPort)
                                }
                            }
                        }

                        // 2. Open Input ports to receive hardware MIDI from keyboard into OP-X
                        info.ports.filter { it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT }.forEach { port ->
                            val outputPort = device.openOutputPort(port.portNumber)
                            outputPort?.connect(object : MidiReceiver() {
                                override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
                                    val data = msg.copyOfRange(offset, offset + count)
                                    onMidiReceived(data)
                                }
                            })
                        }
                    }
                }, mainHandler)
            }

            connectedDeviceNames = if (names.isEmpty()) listOf("No USB-MIDI Device Connected") else names
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning USB MIDI devices", e)
        }
    }

    /**
     * Sends raw MIDI bytes to all connected USB-MIDI hardware ports.
     */
    fun send(bytes: ByteArray) {
        synchronized(activeInputPorts) {
            for (port in activeInputPorts) {
                try {
                    port.send(bytes, 0, bytes.size)
                } catch (e: Exception) {
                    Log.e(TAG, "Error writing to USB-MIDI port", e)
                }
            }
        }
    }

    private fun getDeviceName(info: MidiDeviceInfo): String {
        val props = info.properties
        return props.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
            ?: props.getString(MidiDeviceInfo.PROPERTY_NAME)
            ?: "USB-MIDI Device #${info.id}"
    }

    private fun closeAllPorts() {
        synchronized(activeInputPorts) {
            activeInputPorts.forEach {
                try { it.close() } catch (ignored: Exception) {}
            }
            activeInputPorts.clear()
        }
        synchronized(openDevices) {
            openDevices.forEach {
                try { it.close() } catch (ignored: Exception) {}
            }
            openDevices.clear()
        }
    }

    fun release() {
        try {
            midiManager?.unregisterDeviceCallback(deviceCallback)
        } catch (ignored: Exception) {}
        closeAllPorts()
    }
}
