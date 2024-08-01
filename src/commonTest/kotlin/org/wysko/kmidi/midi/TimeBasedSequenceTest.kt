/*
 * Copyright © 2024 Jacob Wysko
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
import org.wysko.kmidi.midi.TimeBasedSequence.Companion.toTimeBasedSequence
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
}