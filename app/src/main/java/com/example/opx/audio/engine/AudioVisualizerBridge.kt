package com.example.opx.audio.engine

/**
 * Lock-free circular ring buffer bridging the real-time audio thread
 * and the 60fps Compose UI oscilloscope renderer.
 * Avoids any garbage collection allocations inside the audio callback loop.
 */
class AudioVisualizerBridge(val capacity: Int = 512) {
    private val buffer = FloatArray(capacity)
    private var writeIndex = 0

    // Downsample counter to feed oscilloscope at suitable resolution
    private var sampleCounter = 0
    private val downsampleRate = 4 // Keep 1 every 4 frames for smooth wave tracking

    fun writeSample(sample: Float) {
        sampleCounter++
        if (sampleCounter >= downsampleRate) {
            sampleCounter = 0
            buffer[writeIndex] = sample
            writeIndex = (writeIndex + 1) % capacity
        }
    }

    /**
     * Reads a snapshot of recent audio samples into the provided output array.
     */
    fun readSnapshot(output: FloatArray) {
        val count = output.size.coerceAtMost(capacity)
        val start = (writeIndex - count + capacity) % capacity
        for (i in 0 until count) {
            val idx = (start + i) % capacity
            output[i] = buffer[idx]
        }
    }
}
