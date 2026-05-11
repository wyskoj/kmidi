package org.wysko.kmidi.midi

import org.wysko.kmidi.midi.builder.smf
import kotlin.test.Test

class TrackBuilderTest {
    @Test
    fun `Test build track with no events`() {
        smf {
            format = StandardMidiFile.Header.Format.Format0
            division = tpq(96)
            track { }
        }
    }
}