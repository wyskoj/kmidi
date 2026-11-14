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

import org.junit.Before
import org.wysko.kmidi.midi.StandardMidiFile.Header.Format.Format0
import org.wysko.kmidi.midi.TimeBasedSequence.Companion.toTimeBasedSequence
import org.wysko.kmidi.midi.builder.smf
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.kmidi.midi.reader.StandardMidiFileReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class TimeBasedSequenceTest {

    private lateinit var reader: StandardMidiFileReader

    @Before
    fun setUp() {
        reader = StandardMidiFileReader()
    }

    @Test
    fun `Test time based sequence`() {
        val smf = reader.readByteArray(SmfExamples.example1)
        with(smf.toTimeBasedSequence()) {
            val events = smf.tracks.single().events
            val eventsByChannel = events.filterIsInstance<NoteEvent.NoteOn>().groupBy { it.channel }
            eventsByChannel[0.toByte()]?.let { channelEvents ->
                assertEquals(1.seconds, getTimeOf(channelEvents.first()))
            }
            eventsByChannel[1.toByte()]?.let { channelEvents ->
                assertEquals(0.5.seconds, getTimeOf(channelEvents.first()))
            }
            eventsByChannel[2.toByte()]?.let { channelEvents ->
                channelEvents.forEach {
                    assertEquals(Duration.ZERO, getTimeOf(it))
                }
            }
        }
    }

    @Test
    fun `Tempo at time falls back to the default tempo when the file sets none at its start`() {
        val sequence = smf {
            division = tpq(96)
            format = Format0
            track { tempo(90, absoluteTime = 192) }
        }.toTimeBasedSequence()

        assertEquals(120.0, sequence.getTempoAtTime(0.5.seconds).beatsPerMinute, 0.001)
        assertEquals(90.0, sequence.getTempoAtTime(1.seconds).beatsPerMinute, 0.001)
        assertEquals(90.0, sequence.getTempoAtTime(2.seconds).beatsPerMinute, 0.001)
    }
}