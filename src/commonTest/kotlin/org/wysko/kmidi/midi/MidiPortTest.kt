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

import org.wysko.kmidi.midi.StandardMidiFile.Header.Format.Format1
import org.wysko.kmidi.midi.builder.smf
import org.wysko.kmidi.midi.event.MetaEvent
import org.wysko.kmidi.midi.reader.StandardMidiFileReader
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MidiPortTest {
    private val twoPorts =
        smf {
            division = tpq(96)
            format = Format1

            track {
                midiPort(0)
                channel(0) { note("C4", 1.quarter) }
            }

            track {
                midiPort(1)
                channel(0) { note("E4", 1.quarter) }
            }
        }

    @Test
    fun `Test builder adds a MIDI port event`() {
        assertEquals(MetaEvent.MidiPort(0, 1), twoPorts.tracks[1].events.first())
    }

    @Test
    fun `Test builder rejects a port out of range`() {
        assertFailsWith<IllegalArgumentException> {
            smf {
                division = tpq(96)
                track { midiPort(128) }
            }
        }
    }

    @Test
    fun `Test write then read MIDI port`() {
        val bytes = StandardMidiFileWriter().write(twoPorts)
        assertEquals(twoPorts, StandardMidiFileReader().readByteArray(bytes))
    }

    @Test
    fun `Test MIDI port is written as meta event 0x21 with one data byte`() {
        val bytes = StandardMidiFileWriter().write(twoPorts).toList()
        val port = listOf(0xFF, 0x21, 0x01, 0x01).map { it.toByte() }
        assertEquals(true, bytes.windowed(port.size).contains(port))
    }

    @Test
    fun `Test malformed MIDI port is read as unknown`() {
        val malformed = twoPorts.copy(
            tracks = listOf(
                StandardMidiFile.Track(
                    listOf(MetaEvent.Unknown(0, 0x21, byteArrayOf(1, 2)), MetaEvent.EndOfTrack(0)),
                ),
            ),
            header = twoPorts.header.copy(trackCount = 1),
        )
        val event = StandardMidiFileReader().readByteArray(StandardMidiFileWriter().write(malformed))
            .tracks.single().events.first()
        val unknown = event as MetaEvent.Unknown
        assertContentEquals(byteArrayOf(1, 2), unknown.data)
    }
}
