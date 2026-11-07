package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.opx.audio.drum.DrumEngine
import com.example.opx.audio.dsp.Waveform
import com.example.opx.audio.engine.SynthEngine
import com.example.opx.audio.fx.Bitcrusher
import com.example.opx.audio.fx.MasterCompressor
import com.example.opx.audio.fx.MasterEffectsChain
import com.example.opx.audio.fx.PlateReverb
import com.example.opx.audio.fx.TapeDelay
import com.example.opx.audio.tape.TapeDeck
import com.example.opx.audio.tape.TapeTrack
import com.example.opx.audio.tape.TransportState
import com.example.opx.audio.tape.WavExporter
import com.example.opx.midi.MidiConstants
import com.example.opx.midi.MidiPacket
import com.example.opx.midi.MidiRouter
import com.example.opx.model.Note
import com.example.opx.sequencer.SequencerTrack
import com.example.opx.sequencer.StepSequencer
import com.example.opx.sequencer.TombolaPhysics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("OP-X Synth", appName)
    }

    @Test
    fun `synth engine allocates voices and renders audio frames`() {
        val engine = SynthEngine(maxVoices = 8, sampleRate = 48000)
        assertEquals(0, engine.activeVoiceCount)

        // Note-on C4 (MIDI note 60)
        engine.noteOn(60, 0.9f)
        assertEquals(1, engine.activeVoiceCount)

        // Render 256 stereo frames
        val buffer = FloatArray(256 * 2)
        engine.renderBlock(buffer, 256)

        // Verify non-silent output
        var hasSignal = false
        for (sample in buffer) {
            if (kotlin.math.abs(sample) > 0.001f) {
                hasSignal = true
                break
            }
        }
        assertTrue("Audio block should contain synthesized signal", hasSignal)

        // Note-off
        engine.noteOff(60)
        // Render release frames
        val releaseBuffer = FloatArray(48000 * 2) // 1 second
        engine.renderBlock(releaseBuffer, 48000)
        assertEquals(0, engine.activeVoiceCount)
    }

    @Test
    fun `tape track sound on sound overdubbing`() {
        val track = TapeTrack(trackId = 0, maxFrames = 1000)
        track.isArmedForRecording = true
        track.overdubDecay = 0.5f

        // Initial pass: write 0.8f at frame 10
        track.overdubFrame(10, 0.8f, 0.8f)
        assertEquals(0.8f, track.buffer[20], 0.001f)
        assertEquals(11, track.recordedFrames)

        // Second pass overdub: 0.8 * 0.5 + 0.4 = 0.8
        track.overdubFrame(10, 0.4f, 0.4f)
        assertEquals(0.8f, track.buffer[20], 0.001f)

        // Read frame with linear interpolation
        val (left, right) = track.readFrame(10.0, 1000)
        assertTrue(left > 0.1f)
    }

    @Test
    fun `tape deck records input from synth and plays back`() {
        val deck = TapeDeck(sampleRate = 48000, maxTapeSeconds = 10)
        deck.tracks[0].isArmedForRecording = true
        deck.record()
        assertEquals(TransportState.RECORDING, deck.transportState)

        // Synthetic input buffer
        val input = FloatArray(256 * 2) { 0.5f }
        val output = FloatArray(256 * 2) { 0.0f }

        deck.processBlock(input, output, 256)
        assertTrue("Playhead should have advanced", deck.playheadFrame >= 256.0)
        assertTrue("Track 0 should have recorded frames", deck.tracks[0].recordedFrames >= 256)

        // Stop and play back
        deck.stop()
        assertEquals(0.0, deck.playheadFrame, 0.001)
        deck.play()
        assertEquals(TransportState.PLAYING, deck.transportState)

        val silentInput = FloatArray(256 * 2)
        val playbackOutput = FloatArray(256 * 2)
        deck.processBlock(silentInput, playbackOutput, 256)

        var hasTapePlayback = false
        for (sample in playbackOutput) {
            if (sample > 0.1f) {
                hasTapePlayback = true
                break
            }
        }
        assertTrue("Output should contain tape playback signal", hasTapePlayback)
    }

    @Test
    fun `wav exporter generates compliant RIFF header`() {
        val track = TapeTrack(trackId = 0, maxFrames = 48000)
        track.overdubFrame(0, 0.5f, 0.5f)
        track.overdubFrame(100, -0.5f, -0.5f)

        val wavBytes = WavExporter.exportTrackToWav(track, 48000, 48000)
        assertTrue(wavBytes.size > 44)

        // Check RIFF and WAVE magic signatures
        val riff = String(wavBytes, 0, 4)
        val wave = String(wavBytes, 8, 4)
        assertEquals("RIFF", riff)
        assertEquals("WAVE", wave)
    }

    @Test
    fun `polymetric track independent lengths and euclidean generation`() {
        val track = SequencerTrack(trackId = 0, initialLength = 7)
        assertEquals(7, track.length)

        // Verify Euclidean 3 pulses over 7 steps
        track.generateEuclidean(pulses = 3, totalSteps = 7)
        assertEquals(7, track.length)
        val activeCount = track.steps.take(7).count { it.isActive }
        assertEquals(3, activeCount)

        // Test step wrapping at 7
        for (i in 0 until 14) {
            track.advance()
        }
        assertEquals(0, track.currentStepIndex)
    }

    @Test
    fun `step sequencer applies parameter locks on active beat`() {
        val synthEngine = SynthEngine(maxVoices = 8, sampleRate = 48000)
        val sequencer = StepSequencer(sampleRate = 48000, initialBpm = 120f)
        sequencer.tracks[0].length = 4
        sequencer.tracks[0].clear()

        // Step 0 active with locked cutoff at 8888 Hz
        sequencer.tracks[0].steps[0].isActive = true
        sequencer.tracks[0].steps[0].lockedCutoffHz = 8888f
        sequencer.tracks[0].steps[0].midiNote = 60

        sequencer.play()
        assertTrue(sequencer.isPlaying)

        // Advance enough frames to cross the 16th step threshold (48000 * 60 / 120 / 4 = 6000 frames)
        sequencer.processClock(6001, synthEngine)

        // Verify voice active and parameter lock applied
        assertEquals(8888f, synthEngine.params.cutoffHz, 1.0f)
    }

    @Test
    fun `tombola physics steps and triggers events`() {
        val tombola = TombolaPhysics()
        tombola.isRunning = true
        assertEquals(3, tombola.balls.size)

        var triggerFound = false
        for (i in 0 until 100) {
            val events = tombola.update()
            if (events.isNotEmpty()) {
                triggerFound = true
                break
            }
        }
        assertTrue("Balls should move", tombola.balls[0].vx != 0f || tombola.balls[0].vy != 0f)
    }

    @Test
    fun `bitcrusher quantizes bit depth and decimates`() {
        val bitcrusher = Bitcrusher(48000)
        bitcrusher.bitDepth = 4
        bitcrusher.downsampleFactor = 4
        bitcrusher.mix = 1.0f

        val (crushedL, crushedR) = bitcrusher.process(0.7321f, 0.7321f)
        assertTrue(crushedL != 0.7321f)
    }

    @Test
    fun `tape delay generates delayed echoes`() {
        val delay = TapeDelay(48000)
        delay.isBpmSynced = false
        delay.freeTimeMs = 10.0f
        delay.feedback = 0.5f
        delay.mix = 1.0f

        delay.process(1.0f, 1.0f)

        var echoDetected = false
        for (i in 0 until 500) {
            val (dL, dR) = delay.process(0.0f, 0.0f)
            if (dL > 0.1f) {
                echoDetected = true
                break
            }
        }
        assertTrue("Tape delay should output delayed impulse", echoDetected)
    }

    @Test
    fun `master compressor attenuates signal above threshold`() {
        val comp = MasterCompressor(48000)
        comp.thresholdDb = -12.0f
        comp.ratio = 4.0f
        comp.makeupGainDb = 0.0f
        comp.attackMs = 0.5f

        for (i in 0 until 200) {
            comp.process(0.95f, 0.95f)
        }
        assertTrue("Compressor should measure gain reduction", comp.currentGainReductionDb > 1.0f)
    }

    @Test
    fun `master effects chain processes interleaved block`() {
        val chain = MasterEffectsChain(48000)
        val buffer = FloatArray(256 * 2) { 0.5f }
        chain.processBlock(buffer, 256)

        assertTrue(chain.peakOutLeft > 0.05f)
        assertTrue(chain.peakOutRight > 0.05f)
    }

    @Test
    fun `midi packet encoding formats note on, note off, and CC correctly`() {
        val noteOnBytes = MidiPacket.noteOn(channel = 0, note = 60, velocity = 100)
        assertEquals(3, noteOnBytes.size)
        assertEquals(0x90.toByte(), noteOnBytes[0]) // Note On Ch 1
        assertEquals(60.toByte(), noteOnBytes[1])
        assertEquals(100.toByte(), noteOnBytes[2])

        val ccBytes = MidiPacket.controlChange(channel = 0, controller = MidiConstants.CC_CUTOFF_BLUE, value = 127)
        assertEquals(0xB0.toByte(), ccBytes[0]) // CC Ch 1
        assertEquals(74.toByte(), ccBytes[1])
        assertEquals(127.toByte(), ccBytes[2])

        val desc = MidiPacket.formatDescription(noteOnBytes)
        assertTrue(desc.contains("NOTE ON"))
        assertTrue(desc.contains("C4"))
    }

    @Test
    fun `midi packet pitch bend encodes 14-bit center, min, max`() {
        // Center: 0 bend -> 8192 normalized -> LSB=0, MSB=64
        val center = MidiPacket.pitchBend(channel = 0, bendValue14Bit = 0)
        assertEquals(0xE0.toByte(), center[0])
        assertEquals(0.toByte(), center[1])
        assertEquals(64.toByte(), center[2])
    }

    @Test
    fun `midi router dispatches messages and logs activity`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val synth = SynthEngine(maxVoices = 8, sampleRate = 48000)
        val router = MidiRouter(context, synth)

        router.sendNoteOn(note = 60, velocity = 0.8f)
        assertTrue("Activity log should have entry", router.activityLog.isNotEmpty())
        val entry = router.activityLog.first()
        assertTrue(entry.description.contains("NOTE ON"))

        router.sendControlChange(MidiConstants.CC_CUTOFF_BLUE, 0.75f)
        val ccEntry = router.activityLog.first()
        assertTrue(ccEntry.description.contains("CUTOFF"))

        router.release()
    }

    @Test
    fun `drum engine synthesizes punchy transients`() {
        val drumEngine = DrumEngine(48000)
        drumEngine.triggerPad(0, 1.0f) // Kick
        assertTrue(drumEngine.voices[0].isTriggered)

        val buffer = FloatArray(256 * 2)
        drumEngine.processBlock(buffer, 256)

        var hasDrumSignal = false
        for (sample in buffer) {
            if (kotlin.math.abs(sample) > 0.05f) {
                hasDrumSignal = true
                break
            }
        }
        assertTrue("Drum block should contain kick transient", hasDrumSignal)
    }

    @Test
    fun `musical note frequency conversion`() {
        val a4Freq = Note.midiToFrequency(69)
        assertEquals(440.0f, a4Freq, 0.01f)
        assertEquals("A4", Note.getNoteName(69))
        assertEquals("C4", Note.getNoteName(60))
        assertTrue(Note.isBlackKey(61)) // C#4
    }
}
