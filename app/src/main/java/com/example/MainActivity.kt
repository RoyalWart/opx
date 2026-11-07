package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.opx.audio.AudioEngine
import com.example.opx.audio.engine.SynthEngine
import com.example.opx.midi.MidiRouter
import com.example.opx.ui.OPXWorkstationScreen
import com.example.ui.theme.OPXTheme

class MainActivity : ComponentActivity() {
    private lateinit var synthEngine: SynthEngine
    private lateinit var audioEngine: AudioEngine
    private lateinit var midiRouter: MidiRouter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Core Audio & DSP Synthesizer Engine
        synthEngine = SynthEngine(maxVoices = 8)
        audioEngine = AudioEngine(this, synthEngine)
        midiRouter = MidiRouter(this, synthEngine)

        setContent {
            OPXTheme {
                OPXWorkstationScreen(
                    audioEngine = audioEngine,
                    synthEngine = synthEngine,
                    midiRouter = midiRouter,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Automatically start the low-latency audio stream
        if (!audioEngine.isRunning) {
            audioEngine.start()
        }
    }

    override fun onPause() {
        super.onPause()
        // Stop audio playback when app loses focus to conserve power
        audioEngine.stop()
        synthEngine.allNotesOff()
        midiRouter.panicAllNotesOff()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioEngine.release()
        midiRouter.release()
    }
}
