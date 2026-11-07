package com.example.opx.midi

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.Executors

/**
 * High-performance UDP Network MIDI Broadcaster (ipMIDI standard).
 * Broadcasts raw MIDI datagrams over Wi-Fi directly to desktop DAWs (LMMS, Ableton Live, FL Studio).
 */
class NetworkMidiBroadcaster(
    var port: Int = 21928, // Standard ipMIDI port
    var broadcastAddressStr: String = "255.255.255.255"
) {
    companion object {
        private const val TAG = "OPX_NetworkMIDI"
    }

    private var socket: DatagramSocket? = null
    private val sendExecutor = Executors.newSingleThreadExecutor()

    var isEnabled: Boolean = true

    @Volatile
    var packetsSentCount: Long = 0L
        private set

    @Volatile
    var lastError: String? = null
        private set

    @Volatile
    var localIpAddress: String = "Detecting..."
        private set

    init {
        start()
    }

    @Synchronized
    fun start() {
        if (socket != null && !socket!!.isClosed) return

        sendExecutor.execute {
            try {
                detectLocalIp()
                val s = DatagramSocket().apply {
                    broadcast = true
                }
                socket = s
                lastError = null
                Log.i(TAG, "Network MIDI Broadcaster started on port $port, target $broadcastAddressStr")
            } catch (e: Exception) {
                lastError = e.message
                Log.e(TAG, "Failed to start Network MIDI Broadcaster", e)
            }
        }
    }

    /**
     * Sends raw MIDI bytes over UDP broadcast.
     */
    fun send(midiBytes: ByteArray) {
        if (!isEnabled || midiBytes.isEmpty()) return

        sendExecutor.execute {
            try {
                val s = socket ?: return@execute
                val target = InetAddress.getByName(broadcastAddressStr)
                val packet = DatagramPacket(midiBytes, midiBytes.size, target, port)
                s.send(packet)
                packetsSentCount++
            } catch (e: Exception) {
                lastError = e.message
            }
        }
    }

    private fun detectLocalIp() {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr.hostAddress?.contains(':') == false) {
                        localIpAddress = addr.hostAddress ?: "Unknown"
                        return
                    }
                }
            }
            localIpAddress = "127.0.0.1"
        } catch (e: Exception) {
            localIpAddress = "Wi-Fi Not Connected"
        }
    }

    @Synchronized
    fun stop() {
        try {
            socket?.close()
            socket = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket", e)
        }
    }

    fun release() {
        stop()
        sendExecutor.shutdown()
    }
}
