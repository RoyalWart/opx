package com.example.opx.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import android.util.Log
import com.example.opx.audio.drum.DrumEngine
import com.example.opx.audio.engine.SynthEngine
import com.example.opx.audio.fx.MasterEffectsChain
import com.example.opx.audio.tape.TapeDeck
import com.example.opx.sequencer.StepSequencer

/**
 * High-performance low-latency Audio Engine for OP-X.
 *
 * Configures Android's AudioTrack with PERFORMANCE_MODE_LOW_LATENCY and
 * ENCODING_PCM_FLOAT on an URGENT_AUDIO priority thread, targeting <10ms latency.
 * Integrates synth engine, drum sampler, 4-track tape looper, sequencer, and master FX chain.
 */
class AudioEngine(
    context: Context,
    val synthEngine: SynthEngine,
    val drumEngine: DrumEngine = DrumEngine(AudioConfig.detectOptimal(context).sampleRate),
    val tapeDeck: TapeDeck = TapeDeck(AudioConfig.detectOptimal(context).sampleRate),
    val stepSequencer: StepSequencer = StepSequencer(AudioConfig.detectOptimal(context).sampleRate),
    val effectsChain: MasterEffectsChain = MasterEffectsChain(AudioConfig.detectOptimal(context).sampleRate)
) {
    companion object {
        private const val TAG = "OPX_AudioEngine"
    }

    val config: AudioConfig = AudioConfig.detectOptimal(context)

    private var audioTrack: AudioTrack? = null
    private var renderThread: Thread? = null

    @Volatile
    var isRunning: Boolean = false
        private set

    // Interleaved stereo buffers: 2 channels * bufferSizeInFrames floats
    private val bufferSizeFloats = config.bufferSizeInFrames * config.channelCount
    private val synthBuffer = FloatArray(bufferSizeFloats)
    private val renderBuffer = FloatArray(bufferSizeFloats)

    val sampleRate: Int
        get() = config.sampleRate

    val bufferFrames: Int
        get() = config.bufferSizeInFrames

    val estimatedLatencyMs: Float
        get() = config.latencyMs

    val underrunCount: Int
        get() = audioTrack?.underrunCount ?: 0

    init {
        synthEngine.updateSampleRate(config.sampleRate)
    }

    /**
     * Initializes and starts the low-latency audio stream.
     */
    @Synchronized
    fun start() {
        if (isRunning) return

        try {
            val bytesPerSample = 4 // 32-bit IEEE float
            val minBufferSizeInBytes = AudioTrack.getMinBufferSize(
                config.sampleRate,
                config.channelConfig,
                AudioFormat.ENCODING_PCM_FLOAT
            )
            val bufferSizeInBytes = maxOf(
                config.bufferSizeInFrames * config.channelCount * bytesPerSample,
                minBufferSizeInBytes
            )

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setFlags(AudioAttributes.FLAG_LOW_LATENCY)
                .build()

            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setSampleRate(config.sampleRate)
                .setChannelMask(config.channelConfig)
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSizeInBytes)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            track.play()
            isRunning = true

            // Dedicated real-time audio thread
            renderThread = Thread({
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                Log.i(TAG, "Audio thread started at priority URGENT_AUDIO. SR=${config.sampleRate}, BufferFrames=${config.bufferSizeInFrames}")

                while (isRunning) {
                    val currentTrack = audioTrack ?: break

                    // 0. Advance sample-accurate polymetric step sequencer clock
                    stepSequencer.processClock(config.bufferSizeInFrames, synthEngine)

                    // 1. Render stereo PCM float samples from polyphonic synth engine into synthBuffer
                    synthEngine.renderBlock(synthBuffer, config.bufferSizeInFrames)

                    // 1b. Render 8-pad drum transients into synthBuffer
                    drumEngine.processBlock(synthBuffer, config.bufferSizeInFrames)

                    // 2. Prepare renderBuffer with synth & drum output
                    System.arraycopy(synthBuffer, 0, renderBuffer, 0, bufferSizeFloats)

                    // 3. Process 4-track tape deck (overdubs synth into armed tracks & mixes tape playback)
                    tapeDeck.processBlock(synthBuffer, renderBuffer, config.bufferSizeInFrames)

                    // 4. Global Master FX Chain (Bitcrusher -> Tape Delay -> Plate Reverb -> Bus Compressor)
                    effectsChain.processBlock(renderBuffer, config.bufferSizeInFrames)

                    // 5. Write to low-latency stream buffer
                    val written = currentTrack.write(
                        renderBuffer,
                        0,
                        renderBuffer.size,
                        AudioTrack.WRITE_BLOCKING
                    )

                    if (written < 0) {
                        Log.e(TAG, "AudioTrack write error: $written")
                    }
                }
                Log.i(TAG, "Audio thread terminated.")
            }, "OPX-AudioDSP-Thread").apply {
                start()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start low-latency audio stream", e)
            stop()
        }
    }

    /**
     * Stops the audio stream and terminates the rendering thread.
     */
    @Synchronized
    fun stop() {
        if (!isRunning) return
        isRunning = false

        try {
            renderThread?.join(250)
            renderThread = null

            audioTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.release()
            }
            audioTrack = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio stream", e)
        }
    }

    fun release() {
        stop()
        synthEngine.allNotesOff()
        tapeDeck.stop()
        stepSequencer.stop()
        effectsChain.reset()
    }
}
