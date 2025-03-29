package org.wysko.kmidi.midi

import org.junit.Before
import org.junit.Test
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.VirtualCompositePitchBendEvent
import org.wysko.kmidi.midi.reader.StandardMidiFileReader
import org.wysko.kmidi.midi.reader.readFile
import java.io.File

class VirtualEventsTest {
    private lateinit var reader: StandardMidiFileReader

    @Before
    fun setUp() {
        reader = StandardMidiFileReader()
    }

    @Test
    fun `Test get virtual events`() {
        File("src/jvmTest/resources/test_midi").listFiles()!!.filter { it.extension == "mid" }.forEach {
            attemptGetVirtualEvents(reader.readFile(it))
        }
    }

    private fun attemptGetVirtualEvents(smf: StandardMidiFile) {
        repeat(16) { channel ->
            val events = smf
                .tracks
                .flatMap { it.events }
                .filterIsInstance<MidiEvent>()
                .filter { it.channel == channel.toByte() }
            VirtualCompositePitchBendEvent.fromEvents(events)
        }
    }
}
