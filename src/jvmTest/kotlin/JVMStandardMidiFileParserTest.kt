/*
 * Copyright © 2023 Jacob Wysko
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

import org.wysko.kmidi.midi.StandardMidiFileParser
import org.wysko.kmidi.parseInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class JVMStandardMidiFileParserTest {
    @Test
    fun `Test parse file`() {
        val stream = JVMStandardMidiFileParserTest::class.java.getResourceAsStream("/bus_driver.mid")
        if (stream != null) {
            StandardMidiFileParser.parseInputStream(stream)
        } else {
            fail("Could not find resource")
        }
    }

    @Test
    fun `Test track names`() {
        val stream = JVMStandardMidiFileParserTest::class.java.getResourceAsStream("/bus_driver.mid")
        val midiFile = stream?.let { StandardMidiFileParser.parseInputStream(it) } ?: fail("Could not find resource")

        assertEquals(listOf(
            "bass",
            "kit",
            "guit 1",
            "piano",
            "organ",
            "guit 2",
            "brass",
            "tenor sax",
            "tenor sax #2",
        ), midiFile.tracks.mapNotNull { it.name })
    }
}
