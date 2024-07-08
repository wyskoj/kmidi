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

package org.wysko.kmidi.midi

import org.wysko.kmidi.midi.event.MetaEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.kmidi.midi.event.ProgramEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class StandardMidiFileReaderTest {
    companion object {
        private val smfExpected1 =
            StandardMidiFile(
                StandardMidiFile.Header(
                    "MThd",
                    StandardMidiFile.Header.Format.Format0,
                    1,
                    StandardMidiFile.Header.Division.MetricalTime(96),
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
                            MetaEvent.EndOfTrack(0x60 * 2 + 0xC0),
                        ),
                    ),
                ),
            )
    }

    @Test
    fun testParseExample() {
        val smf = StandardMidiFileReader().readByteArray(SmfExamples.example1)
        assertEquals(smfExpected1, smf)
    }
}
