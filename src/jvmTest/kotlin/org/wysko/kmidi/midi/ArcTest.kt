package org.wysko.kmidi.midi

import org.junit.Before
import org.wysko.kmidi.midi.TimeBasedSequence.Companion.toTimeBasedSequence
import org.wysko.kmidi.midi.reader.StandardMidiFileReader
import kotlin.test.Test

class ArcTest {
    private lateinit var reader: StandardMidiFileReader

    @Before
    fun setUp() {
        reader = StandardMidiFileReader()
    }

    @Test
    fun `Test arcs from notes`() {
        val smf = reader.readByteArray(SmfExamples.example1)
        val sequence = smf.toTimeBasedSequence()
        val arcs = sequence.convertArcsToTimedArcs(smf.tracks.single().arcs)

        assert(arcs.size == 4)
    }
}
