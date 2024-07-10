package org.wysko.kmidi.midi

import org.wysko.kmidi.midi.StandardMidiFile.Header.Format.Format1
import org.wysko.kmidi.midi.builder.smf
import kotlin.test.Test

class StandardMidiFileBuilderTest {
    @Test
    fun `Test build SMF`() {
        smf {
            division = tpq(96)
            format = Format1

            track {
                tempo(100)
            }

            track {
                channel(0) {
                    program(65)
                    note("D4", 1.eighth)
                    note("E4", 1.eighth)
                    note("F4", 1.eighth)
                    note("G4", 1.eighth)
                    note("E4", 1.quarter)
                    note("C4", 1.eighth)
                    note("D4", 1.eighth + 1.half)
                }
            }
        }
    }
}
