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

package org.wysko.kmidi.midi.analysis

import org.wysko.kmidi.midi.event.NoteEvent
import kotlin.test.Test
import kotlin.test.assertEquals

class PolyphonyTest {
    @Test
    fun `Test calculateMaximumPolyphony empty list`() {
        val events = emptyList<NoteEvent>()
        val result = Polyphony.calculateMaximumPolyphony(events)
        assertEquals(0, result)
    }

    @Test
    fun `Test calculateMaximumPolyphony single note`() {
        val events =
            listOf(
                NoteEvent.NoteOn(0, 0, 60, 127),
                NoteEvent.NoteOff(1, 0, 60, 0),
            )
        val result = Polyphony.calculateMaximumPolyphony(events)
        assertEquals(1, result)
    }

    @Test
    fun `Test calculateMaximumPolyphony two simultaneous notes`() {
        val events =
            listOf(
                NoteEvent.NoteOn(0, 0, 60, 127),
                NoteEvent.NoteOn(0, 0, 61, 127),
                NoteEvent.NoteOff(1, 0, 60, 0),
                NoteEvent.NoteOff(1, 0, 61, 0),
            )
        val result = Polyphony.calculateMaximumPolyphony(events)
        assertEquals(2, result)
    }

    @Test
    fun `Test calculateMaximumPolyphony two notes with overlap`() {
        val events =
            listOf(
                NoteEvent.NoteOn(0, 0, 60, 127),
                NoteEvent.NoteOn(1, 0, 61, 127),
                NoteEvent.NoteOff(2, 0, 60, 0),
                NoteEvent.NoteOff(3, 0, 61, 0),
            )
        val result = Polyphony.calculateMaximumPolyphony(events)
        assertEquals(2, result)
    }

    @Test
    fun `Test averagePolyphony empty list`() {
        val events = emptyList<NoteEvent>()
        val result = Polyphony.averagePolyphony(events)
        assertEquals(0.0, result)
    }

    @Test
    fun `Test averagePolyphony single note`() {
        val events =
            listOf(
                NoteEvent.NoteOn(0, 0, 60, 127),
                NoteEvent.NoteOff(1, 0, 60, 0),
            )
        val result = Polyphony.averagePolyphony(events)
        assertEquals(1.0, result)
    }

    @Test
    fun `Test averagePolyphony two simultaneous notes`() {
        val events =
            listOf(
                NoteEvent.NoteOn(0, 0, 60, 127),
                NoteEvent.NoteOn(0, 0, 61, 127),
                NoteEvent.NoteOff(1, 0, 60, 0),
                NoteEvent.NoteOff(1, 0, 61, 0),
            )
        val result = Polyphony.averagePolyphony(events)
        assertEquals(2.0, result)
    }

    @Test
    fun `Test averagePolyphony two notes with overlap`() {
        val events =
            listOf(
                NoteEvent.NoteOn(0, 0, 60, 127),
                NoteEvent.NoteOn(1, 0, 61, 127),
                NoteEvent.NoteOff(2, 0, 60, 0),
                NoteEvent.NoteOff(3, 0, 61, 0),
            )
        val result = Polyphony.averagePolyphony(events)
        assertEquals(4 / 3.0, result, 0.0001)
    }
}
