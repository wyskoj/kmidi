package org.wysko.kmidi.midi

import org.junit.Test
import org.wysko.kmidi.midi.builder.smf
import org.wysko.kmidi.midi.reader.StandardMidiFileReader
import kotlin.test.assertEquals

class TimecodeBasedTimeTest {
    @Test
    fun `Test SMF with timecode-based time roundtrip`() {
        val reader = StandardMidiFileReader()
        val writer = StandardMidiFileWriter()

        val smpteTimes = listOf(-24, -25, -29, -30)
        val ticks = (1..127)

        for (smpteTime in smpteTimes) {
            for (tick in ticks) {
                val smf = smf {
                    format = StandardMidiFile.Header.Format.Format0
                    division = StandardMidiFile.Header.Division.TimecodeBasedTime(
                        framesPerSecond = smpteTime.toShort(),
                        ticksPerFrame = tick.toShort()
                    )

                    track {
                        note(60, 1.quarter)
                    }
                }

                val bytes = writer.write(smf)
                val smfRoundTrip = reader.readByteArray(bytes)

                assertEquals(smf, smfRoundTrip)
            }
        }
    }
}