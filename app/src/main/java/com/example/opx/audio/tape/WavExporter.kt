package com.example.opx.audio.tape

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * High-performance WAV file exporter for 4-track tape stems.
 * Formats stereo float buffers into standard 16-bit PCM WAV audio for DAWs (LMMS, Ableton, FL Studio).
 */
object WavExporter {

    /**
     * Converts a single TapeTrack's recorded audio into a 16-bit stereo WAV byte array.
     */
    fun exportTrackToWav(track: TapeTrack, sampleRate: Int, lengthFrames: Int): ByteArray {
        val effectiveFrames = minOf(lengthFrames, track.recordedFrames).coerceAtLeast(sampleRate) // At least 1 sec
        val pcmData = ByteBuffer.allocate(effectiveFrames * 2 * 2).order(ByteOrder.LITTLE_ENDIAN)

        for (f in 0 until effectiveFrames) {
            val idx = f * 2
            val left = track.buffer[idx].coerceIn(-1.0f, 1.0f)
            val right = track.buffer[idx + 1].coerceIn(-1.0f, 1.0f)

            pcmData.putShort((left * 32767.0f).toInt().toShort())
            pcmData.putShort((right * 32767.0f).toInt().toShort())
        }

        return createWavContainer(pcmData.array(), sampleRate, channels = 2)
    }

    /**
     * Mixes all 4 tracks into a combined stereo WAV mixdown.
     */
    fun exportMixdownToWav(deck: TapeDeck, sampleRate: Int): ByteArray {
        val effectiveFrames = deck.loopEndFrame.coerceIn(sampleRate, deck.totalFrames)
        val pcmData = ByteBuffer.allocate(effectiveFrames * 2 * 2).order(ByteOrder.LITTLE_ENDIAN)

        for (f in 0 until effectiveFrames) {
            var sumLeft = 0.0f
            var sumRight = 0.0f

            for (t in 0..3) {
                val track = deck.tracks[t]
                if (!track.isMuted) {
                    val idx = f * 2
                    sumLeft += track.buffer[idx] * track.volume * (1.0f - track.pan)
                    sumRight += track.buffer[idx + 1] * track.volume * track.pan
                }
            }

            // Soft-limit master output
            val limitedLeft = kotlin.math.tanh(sumLeft.toDouble()).toFloat()
            val limitedRight = kotlin.math.tanh(sumRight.toDouble()).toFloat()

            pcmData.putShort((limitedLeft * 32767.0f).toInt().toShort())
            pcmData.putShort((limitedRight * 32767.0f).toInt().toShort())
        }

        return createWavContainer(pcmData.array(), sampleRate, channels = 2)
    }

    /**
     * Builds the standard RIFF / WAVE header conforming to RFC specifications.
     */
    fun createWavContainer(pcmBytes: ByteArray, sampleRate: Int, channels: Int = 2): ByteArray {
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val blockAlign = channels * (bitsPerSample / 8)
        val dataLength = pcmBytes.size
        val totalLength = 36 + dataLength

        val out = ByteArrayOutputStream(44 + dataLength)
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF chunk
        header.put("RIFF".toByteArray())
        header.putInt(totalLength)
        header.put("WAVE".toByteArray())

        // fmt chunk
        header.put("fmt ".toByteArray())
        header.putInt(16) // Subchunk1Size for PCM
        header.putShort(1.toShort()) // AudioFormat 1 = PCM
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(blockAlign.toShort())
        header.putShort(bitsPerSample.toShort())

        // data chunk
        header.put("data".toByteArray())
        header.putInt(dataLength)

        out.write(header.array())
        out.write(pcmBytes)
        return out.toByteArray()
    }

    /**
     * Writes all 4 stems into a target directory (e.g. context.cacheDir or external files).
     */
    fun exportAllStemsToDirectory(deck: TapeDeck, targetDir: File): List<File> {
        if (!targetDir.exists()) targetDir.mkdirs()
        val generatedFiles = mutableListOf<File>()

        for (i in 0..3) {
            val trackFile = File(targetDir, "OP-X_Stem_Track_${i + 1}.wav")
            val wavBytes = exportTrackToWav(deck.tracks[i], deck.sampleRate, deck.loopEndFrame)
            FileOutputStream(trackFile).use { it.write(wavBytes) }
            generatedFiles.add(trackFile)
        }

        val mixFile = File(targetDir, "OP-X_Master_Mixdown.wav")
        val mixBytes = exportMixdownToWav(deck, deck.sampleRate)
        FileOutputStream(mixFile).use { it.write(mixBytes) }
        generatedFiles.add(mixFile)

        return generatedFiles
    }
}
