package com.example.opx.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager

/**
 * Low-latency audio configuration parameters.
 * Queries device hardware properties for native sample rate and optimal buffer sizes.
 */
data class AudioConfig(
    val sampleRate: Int = 48000,
    val bufferSizeInFrames: Int = 256,
    val channelCount: Int = 2,
    val isLowLatencySupported: Boolean = true
) {
    val latencyMs: Float
        get() = (bufferSizeInFrames.toFloat() / sampleRate.toFloat()) * 1000f

    val channelConfig: Int = if (channelCount == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO

    companion object {
        /**
         * Detects the optimal native hardware audio configuration to avoid resampling
         * and achieve the lowest possible buffer latency on Android.
         */
        fun detectOptimal(context: Context): AudioConfig {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val nativeSampleRateStr = audioManager?.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            val nativeFramesPerBufferStr = audioManager?.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)

            val sampleRate = nativeSampleRateStr?.toIntOrNull() ?: 48000
            val bufferSize = nativeFramesPerBufferStr?.toIntOrNull() ?: 256

            val hasLowLatency = context.packageManager.hasSystemFeature("android.hardware.audio.low_latency")
            val hasProAudio = context.packageManager.hasSystemFeature("android.hardware.audio.pro")

            return AudioConfig(
                sampleRate = sampleRate,
                bufferSizeInFrames = bufferSize.coerceIn(64, 512),
                channelCount = 2,
                isLowLatencySupported = hasLowLatency || hasProAudio
            )
        }
    }
}
