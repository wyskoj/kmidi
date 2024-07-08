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
                NoteEvent.NoteOff(1, 0, 60),
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
                NoteEvent.NoteOff(1, 0, 60),
                NoteEvent.NoteOff(1, 0, 61),
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
                NoteEvent.NoteOff(2, 0, 60),
                NoteEvent.NoteOff(3, 0, 61),
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
                NoteEvent.NoteOff(1, 0, 60),
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
                NoteEvent.NoteOff(1, 0, 60),
                NoteEvent.NoteOff(1, 0, 61),
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
                NoteEvent.NoteOff(2, 0, 60),
                NoteEvent.NoteOff(3, 0, 61),
            )
        val result = Polyphony.averagePolyphony(events)
        assertEquals(4 / 3.0, result, 0.0001)
    }
}
