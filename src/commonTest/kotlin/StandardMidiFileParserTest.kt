/*
 * Copyright © 2023 Jacob Wysko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.wysko.kmidi.midi.StandardMidiFile
import org.wysko.kmidi.midi.StandardMidiFileParser
import org.wysko.kmidi.midi.event.MetaEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.kmidi.midi.event.ProgramEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class StandardMidiFileParserTest {
    companion object {
        private val smfExample1 = intArrayOf(
            0x4D, 0x54, 0x68, 0x64,
            0x00, 0x00, 0x00, 0x06,
            0x00, 0x00,
            0x00, 0x01,
            0x00, 0x60,
            0x4D, 0x54, 0x72, 0x6B,
            0x00, 0x00, 0x00, 0x3B,
            0x00, 0xFF, 0x58, 0x04, 0x04, 0x02, 0x18, 0x08,
            0x00, 0xFF, 0x51, 0x03, 0x07, 0xA1, 0x20,
            0x00, 0xC0, 0x05,
            0x00, 0xC1, 0x2E,
            0x00, 0xC2, 0x46,
            0x00, 0x92, 0x30, 0x60,
            0x00, 0x3C, 0x60,
            0x60, 0x91, 0x43, 0x40,
            0x60, 0x90, 0x4C, 0x20,
            0x81, 0x40, 0x82, 0x30, 0x40,
            0x00, 0x3C, 0x40,
            0x00, 0x81, 0x43, 0x40,
            0x00, 0x80, 0x4C, 0x40,
            0x00, 0xFF, 0x2F, 0x00,
        ).map { it.toByte() }.toByteArray()
        private val smfExpected1 = StandardMidiFile(
            StandardMidiFile.Header(
                "MThd",
                StandardMidiFile.Header.Format.Format0,
                1,
                StandardMidiFile.Header.Division.MetricalTime(96)
            ),
            listOf(
                StandardMidiFile.Track(
                    listOf(
                        MetaEvent.TimeSignature(0, 4, 2, 24, 8),
                        MetaEvent.SetTempo(0, 500000),
                        ProgramEvent(0, 0, 5),
                        ProgramEvent(0, 1, 0x2E),
                        ProgramEvent(0, 2, 0x46),
                        NoteEvent.NoteOn(0, 2, 0x30, 0x60),
                        NoteEvent.NoteOn(0, 2, 0x3C, 0x60),
                        NoteEvent.NoteOn(0x60, 1, 0x43, 0x40),
                        NoteEvent.NoteOn(0x60 + 0x60, 0, 0x4C, 0x20),
                        NoteEvent.NoteOff(0x60 * 2 + 0xC0, 2, 0x30),
                        NoteEvent.NoteOff(0x60 * 2 + 0xC0, 2, 0x3C),
                        NoteEvent.NoteOff(0x60 * 2 + 0xC0, 1, 0x43),
                        NoteEvent.NoteOff(0x60 * 2 + 0xC0, 0, 0x4C),
                        MetaEvent.EndOfTrack(0x60 * 2 + 0xC0)
                    )
                )
            )
        )
    }

    @Test
    fun testParseExample() {
        val smf = StandardMidiFileParser.parseByteArray(smfExample1)
        assertEquals(smfExpected1, smf)

        smf.tracks.first().events.first().time
    }
}
