# ARCHITECTURAL SPECIFICATION: `OP-X ANDROID ENGINE`
**System Name:** OP-X Mobile Synthetic Workstation & Touch Controller  
**Target Platform:** Android (Tablet & Phone Form Factors)  
**Design Target:** Open-source software alternative to Teenage Engineering OP-1 / OP-Z for Mobile Devices  
**Primary Roles:** 
1. Low-Latency DSP Synthesizer, SoundFont Player & Sampler
2. Multi-Track Touch Looper & Audio/MIDI Tape Deck
3. Universal USB & Network MIDI Controller for DAWs (LMMS, Ableton, FL Studio Mobile)

---

## 1. System Overview & Android Stack

| Layer | Recommended Technology | Purpose |
| :--- | :--- | :--- |
| **UI / Framework** | **Flutter (Dart)** or **Jetpack Compose (Kotlin)** | High-fps responsive UI, custom vector canvases, touch-gesture handling |
| **Audio Engine (DSP)** | **C++ via Android NDK (Oboe / AAudio API)** | Low-latency audio rendering (<10ms latency target), custom synth algorithms |
| **MIDI Routing** | **Android MIDI API (`android.media.midi`)** | USB-MIDI Host/Peripheral mode & Network MIDI over Wi-Fi |
| **Storage / Export** | **Android SAF (Storage Access Framework)** | WAV Tape stem export, SoundFont (.sf2) import, preset management |

---

## 2. Low-Latency Audio & DSP Engine (Oboe / C++)

To achieve teenage engineering-style real-time response on Android without audio dropouts, audio processing is decoupled from the UI thread using a native C++ audio stream via Google's **Oboe** library.

### 2.1 Synth Engines (Swappable Slots)
1. **Cluster Synth Engine:** 6-Voice virtual analog (Saw/Pulse/Sub) with detune and unison.
2. **FM/Phase Modulation:** 2-Op FM engine with dynamic feedback and mod index mapped to touch controls.
3. **Karplus-Strong Engine:** Physical acoustic string simulation.
4. **Wavetable Morph Engine:** 2D scanning across wavetables via multi-touch touchpads.

### 2.2 Sampler & SoundFont Engine
* **RAM Sample Engine:** Instant transient slicing for drum kits across screen pads.
* **Granular Sampler Engine:** Grain position, density, and pitch jitter controlled via touch drag gestures.
* **SoundFont (.sf2) Engine:** Embedded SoundFont reader for acoustic instrument simulation.

### 2.3 Master Processing & FX Chain
* **Chain Sequence:** ADSR Envelope → 4-Pole Resonant Filter → Overdrive → Dynamic Delay / Reverb → Master Limiter.

---

## 3. Mobile Touch UI & Color Encoders

The UI is optimized for multi-touch performance, simulating the four signature color encoders using interactive visual touch dials and gestural pads:

```
 ┌──────────────────────────────────────────────────────────────┐
 │ [ SYNTH ]    [ DRUM ]    [ TAPE ]    [ MIXER ]   [ MIDI OUT ]│  <-- Top Tab Bar
 ├──────────────────────────────────────────────────────────────┤
 │                                                              │
 │               INTERACTIVE VECTOR DISPLAY CANVAS              │  <-- Real-time waveform /
 │               (60 FPS Flutter / Canvas Painter)              │      tape loop visualizer
 │                                                              │
 ├──────────────────────────────────────────────────────────────┤
 │  ( BLUE )     ( OCHRE )     ( SLATE )     ( ORANGE )         │  <-- 4 Touch Encoders
 │  Cutoff       Resonance      FX Send       Modulation        │      (Vertical Drag / Wheel)
 ├──────────────────────────────────────────────────────────────┤
 │  [ C1 ] [ D1 ] [ E1 ] [ F1 ] [ G1 ] [ A1 ] [ B1 ] [ C2 ] ...  │  <-- Responsive Touch
 └──────────────────────────────────────────────────────────────┘      Piano / Drum Pads
```

* **Color Palette:** High-contrast dark theme featuring standard OP-1 accent colors: **Cyan Blue (`#00A3E0`)**, **Mustard Ochre (`#E5A100`)**, **Slate Gray (`#708090`)**, and **Safety Orange (`#FF5500`)**.
* **Gestures:** Two-finger pinch for tape scrubbing, horizontal swipe for pitch bend, long-press for step parameter lock.

---

## 4. Multi-Track Tape Looper & Sequencer Subsystem

### 4.1 4-Track Virtual Tape
* **4-Track Multi-Track Recording:** Record direct audio or synth engine output into 4 synced stereo tracks.
* **Tape FX:** Sound-on-sound overdubbing, variable speed scrubbing (-200% to +200%), tape stop simulation, reverse play.
* **Stem Export:** Export recorded tape tracks directly to WAV files via Android Storage Access Framework for instant loading into desktop DAWs (LMMS, Ableton).

### 4.2 Step Sequencer
* **16-Step Polymetric Sequencer:** Set dynamic track lengths per synth layer.
* **Parameter Locks:** Touch any step to lock specific filter, pitch, or FX settings to that individual beat.

---

## 5. DAW Integration & Controller Mode (LMMS Bridge)

When connected to a PC/Mac running **LMMS**, **Ableton**, or **FL Studio**, OP-X switches to USB Peripheral or Wi-Fi Network MIDI Controller mode.

```
┌─────────────────────────────────┐
│     OP-X Mobile (Android)       │
└────────────────┬────────────────┘
                 │
  ┌──────────────┴──────────────┐
  │                             │
  ▼ (USB-C OTG Cable)           ▼ (Wi-Fi / ipMIDI Network)
[ USB Class-Compliant MIDI ]   [ UDP Network MIDI Broadcast ]
  │                             │
  └──────────────┬──────────────┘
                 ▼
 ┌────────────────────────────────┐
 │   Desktop DAW (LMMS / PC)      │
 └────────────────────────────────┘
```

### 5.1 Android MIDI Mapping Table

| Touch Control | MIDI Message / Channel | LMMS Control Assignment |
| :--- | :--- | :--- |
| **Touch Keyboard / Pads** | Note On / Note Off (Ch 1) | Active Track Instrument Play |
| **Blue Touch Encoder** | CC 74 (Filter Cutoff) | LMMS Track Cutoff / Channel Vol |
| **Ochre Touch Encoder** | CC 71 (Resonance) | LMMS Synth Resonance / EQ High |
| **Slate Touch Encoder** | CC 16 (General CC 1) | LMMS FX Send 1 / Reverb Level |
| **Orange Touch Encoder**| CC 17 (General CC 2) | LMMS FX Send 2 / Delay Level |
| **Transport Keys** | MMC (Play/Stop/Rec) | LMMS Global Transport Sync |

---

## 6. Prompt Engineering Guide for Google AI Studio

To build this Android app using **Google AI Studio**:

1. Open **Google AI Studio** and select **Gemini 1.5 Pro**.
2. Set **System Instructions** to:
   > *"You are an expert mobile audio software architect specializing in Android audio development, Flutter UI, and Android NDK (Oboe/AAudio) low-latency DSP synthesis."*
3. Upload or paste this `opx_android_specification.md` file into the prompt window.
4. Prompt the model:
   > *"Generate a complete single-file Flutter prototype (main.dart) based on `opx_android_specification.md` featuring 4 interactive color encoders, multi-touch keyboard triggering, virtual tape looper controls, and MIDI output via `flutter_midi_command`."*