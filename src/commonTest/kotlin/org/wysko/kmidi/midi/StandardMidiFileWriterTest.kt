/*
 * Copyright © 2026 Jacob Wysko
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

import junit.framework.TestCase.assertEquals
import org.wysko.kmidi.midi.StandardMidiFile.Header.Format.Format1
import org.wysko.kmidi.midi.builder.smf
import org.wysko.kmidi.midi.event.MetaEvent
import org.wysko.kmidi.midi.reader.StandardMidiFileReader
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals

class StandardMidiFileWriterTest {
    private lateinit var smf: StandardMidiFile

    @BeforeTest
    fun setUp() {
        smf =
            smf {
                division = tpq(96)
                format = Format1

                track {
                    tempo(100)
                }

                track {
                    channel(0) {
                        program(65)
                        note("D4", 1.eighth)
                        note("E4", 1.eighth)
                        note("F4", 1.eighth)
                        note("G4", 1.eighth)
                        note("E4", 1.quarter)
                        note("C4", 1.eighth)
                        note("D4", 1.eighth + 1.half)
                    }
                }
            }
    }

    @Test
    fun `Test write then read SMF`() {
        val writer = StandardMidiFileWriter()
        val bytes = writer.write(smf)
        val reader = StandardMidiFileReader()
        val smfRead = reader.readByteArray(bytes)
        assertEquals(smf, smfRead)
    }

    @Test
    fun `Test write then read sequencer-specific and unknown meta events keeps their data`() {
        val track = StandardMidiFile.Track(
            listOf(
                MetaEvent.SequencerSpecific(0, byteArrayOf(0x43, 0x7B)),
                MetaEvent.Unknown(0, 0x60, byteArrayOf(1, 2, 3)),
                MetaEvent.EndOfTrack(0),
            ),
        )
        val oneTrack = smf.copy(header = smf.header.copy(trackCount = 1), tracks = listOf(track))
        val events = StandardMidiFileReader().readByteArray(StandardMidiFileWriter().write(oneTrack)).tracks.single().events

        assertContentEquals(byteArrayOf(0x43, 0x7B), (events[0] as MetaEvent.SequencerSpecific).data)
        assertContentEquals(byteArrayOf(1, 2, 3), (events[1] as MetaEvent.Unknown).data)
    }
}
