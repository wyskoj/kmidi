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
