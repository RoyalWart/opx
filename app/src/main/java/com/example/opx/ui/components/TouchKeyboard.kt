package com.example.opx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.opx.model.Note
import com.example.ui.theme.ChassisBorder
import com.example.ui.theme.KeyBlack
import com.example.ui.theme.KeyBlackPressed
import com.example.ui.theme.KeyWhite
import com.example.ui.theme.KeyWhitePressed
import com.example.ui.theme.SafetyOrange
import com.example.ui.theme.TextMuted

/**
 * Responsive Multi-Touch Piano Keyboard for OP-X.
 * Accurately tracks simultaneous touches for polyphonic chords and solos.
 */
@Composable
fun TouchKeyboard(
    baseMidiNote: Int = 60, // C4
    numWhiteKeys: Int = 14, // 2 full octaves (C to B twice)
    onNoteOn: (Int) -> Unit,
    onNoteOff: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeNotes = remember { mutableStateMapOf<Int, Int>() } // Note -> count of touching fingers
    val pointerToNoteMap = remember { mutableMapOf<PointerId, Int>() }

    val onNoteOnUpdated = rememberUpdatedState(onNoteOn)
    val onNoteOffUpdated = rememberUpdatedState(onNoteOff)

    // Build the white and black key layout relative to baseMidiNote
    // White key indices
    val whiteNotes = remember(baseMidiNote, numWhiteKeys) {
        val list = mutableListOf<Int>()
        var current = baseMidiNote
        while (list.size < numWhiteKeys) {
            if (!Note.isBlackKey(current)) {
                list.add(current)
            }
            current++
        }
        list
    }

    BoxWithConstraints(
        modifier = modifier
            .testTag("touch_keyboard")
            .height(130.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0F1012))
            .border(1.dp, ChassisBorder, RoundedCornerShape(6.dp))
            .pointerInput(baseMidiNote, numWhiteKeys) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val widthPx = size.width.toFloat()
                        val heightPx = size.height.toFloat()
                        val whiteKeyWidth = widthPx / numWhiteKeys.toFloat()
                        val blackKeyHeight = heightPx * 0.60f
                        val blackKeyWidth = whiteKeyWidth * 0.62f

                        val currentDownPointerIds = mutableSetOf<PointerId>()

                        for (change in event.changes) {
                            if (change.pressed) {
                                currentDownPointerIds.add(change.id)
                                val pos = change.position

                                // Determine which key is at position (x, y)
                                // 1. First test if hitting any black key in top 60%
                                var hitNote: Int? = null
                                if (pos.y in 0f..blackKeyHeight) {
                                    for (i in 0 until whiteNotes.size - 1) {
                                        val wNote = whiteNotes[i]
                                        val nextPotentialBlack = wNote + 1
                                        if (Note.isBlackKey(nextPotentialBlack)) {
                                            val keyCenterX = (i + 1) * whiteKeyWidth
                                            val minX = keyCenterX - (blackKeyWidth / 2f)
                                            val maxX = keyCenterX + (blackKeyWidth / 2f)
                                            if (pos.x in minX..maxX) {
                                                hitNote = nextPotentialBlack
                                                break
                                            }
                                        }
                                    }
                                }

                                // 2. If not black key, find white key
                                if (hitNote == null && pos.y in 0f..heightPx && pos.x in 0f..widthPx) {
                                    val whiteIndex = (pos.x / whiteKeyWidth).toInt().coerceIn(0, whiteNotes.size - 1)
                                    hitNote = whiteNotes[whiteIndex]
                                }

                                val prevNote = pointerToNoteMap[change.id]
                                if (hitNote != null && hitNote != prevNote) {
                                    // Release previous note for this finger
                                    if (prevNote != null) {
                                        val count = (activeNotes[prevNote] ?: 1) - 1
                                        if (count <= 0) {
                                            activeNotes.remove(prevNote)
                                            onNoteOffUpdated.value(prevNote)
                                        } else {
                                            activeNotes[prevNote] = count
                                        }
                                    }
                                    // Trigger new note
                                    pointerToNoteMap[change.id] = hitNote
                                    val currentCount = activeNotes[hitNote] ?: 0
                                    activeNotes[hitNote] = currentCount + 1
                                    if (currentCount == 0) {
                                        onNoteOnUpdated.value(hitNote)
                                    }
                                }
                            }
                        }

                        // Check pointers that were released
                        val releasedPointers = pointerToNoteMap.keys.filter { it !in currentDownPointerIds }
                        for (releasedId in releasedPointers) {
                            val note = pointerToNoteMap.remove(releasedId)
                            if (note != null) {
                                val count = (activeNotes[note] ?: 1) - 1
                                if (count <= 0) {
                                    activeNotes.remove(note)
                                    onNoteOffUpdated.value(note)
                                } else {
                                    activeNotes[note] = count
                                }
                            }
                        }
                    }
                }
            }
    ) {
        val totalWidth = maxWidth
        val keyWidth = totalWidth / numWhiteKeys

        // 1. Render White Keys (Bottom Layer)
        Box(modifier = Modifier.fillMaxSize()) {
            whiteNotes.forEachIndexed { index, midiNote ->
                val isPressed = (activeNotes[midiNote] ?: 0) > 0
                val keyColor = if (isPressed) SafetyOrange else KeyWhite
                val textColor = if (isPressed) Color.White else TextMuted

                Box(
                    modifier = Modifier
                        .offset(x = keyWidth * index)
                        .width(keyWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 0.8.dp, vertical = 1.dp)
                        .shadow(if (isPressed) 0.dp else 2.dp, RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                        .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                        .background(if (isPressed) keyColor else KeyWhitePressed)
                        .border(
                            0.5.dp,
                            if (isPressed) SafetyOrange else Color(0xFFC0C5CF),
                            RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = Note.getNoteName(midiNote),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }

        // 2. Render Black Keys (Top Layer)
        val blackKeyWidth = keyWidth * 0.62f
        val blackKeyHeight = 78.dp

        Box(modifier = Modifier.fillMaxSize()) {
            whiteNotes.forEachIndexed { index, whiteMidi ->
                val blackMidi = whiteMidi + 1
                if (Note.isBlackKey(blackMidi) && index < whiteNotes.size - 1) {
                    val isPressed = (activeNotes[blackMidi] ?: 0) > 0
                    val keyCenterOffset = (keyWidth * (index + 1)) - (blackKeyWidth / 2f)

                    Box(
                        modifier = Modifier
                            .offset(x = keyCenterOffset)
                            .width(blackKeyWidth)
                            .height(blackKeyHeight)
                            .shadow(if (isPressed) 0.dp else 4.dp, RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                            .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                            .background(if (isPressed) SafetyOrange else KeyBlack)
                            .border(
                                1.dp,
                                if (isPressed) SafetyOrange else KeyBlackPressed,
                                RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)
                            ),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = Note.getNoteName(blackMidi).replace("#", "♯"),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPressed) Color.White else Color(0xFF888E99),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
