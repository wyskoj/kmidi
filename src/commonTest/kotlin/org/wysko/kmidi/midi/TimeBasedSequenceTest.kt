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