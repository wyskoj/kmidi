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

import org.junit.Before
import org.wysko.kmidi.midi.StandardMidiFileReader
import org.wysko.kmidi.readFile
import org.wysko.kmidi.readInputStream
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.fail

class JVMStandardMidiFileReaderTest {
    private lateinit var reader: StandardMidiFileReader

    @Before
    fun setUp() {
        reader = StandardMidiFileReader()
    }

    @Test
    fun `Test parse file`() {
        reader.readFile(File("src/jvmTest/resources/test_midi/bus_driver.mid"))
    }

    @Test
    fun `Test parse file with input stream`() {
        JVMStandardMidiFileReaderTest::class.java.getResourceAsStream("/test_midi/bus_driver.mid")?.let {
            reader.readInputStream(it)
        } ?: fail("Could not find resource")
    }

    @Test
    fun `Test track names`() {
        val stream = JVMStandardMidiFileReaderTest::class.java.getResourceAsStream("/test_midi/bus_driver.mid")
        val midiFile = stream?.let { reader.readInputStream(it) } ?: fail("Could not find resource")

        assertEquals(
            listOf(
                "bass",
                "kit",
                "guit 1",
                "piano",
                "organ",
                "guit 2",
                "brass",
                "tenor sax",
                "tenor sax #2",
            ),
            midiFile.tracks.mapNotNull { it.name },
        )
    }

    @Test
    fun `Test parse and fail non-MIDI file`() {
        assertFails {
            reader.readFile(File("src/jvmTest/resources/test_midi/example.txt"))
        }
    }
}
