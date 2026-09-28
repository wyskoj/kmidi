/*
 * Copyright © 2026 Jacob Wysko
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

import org.wysko.kmidi.midi.event.MetaEvent
import org.wysko.kmidi.midi.reader.StandardMidiFileReader
import kotlin.test.Test
import kotlin.test.assertEquals

class MetaEventTextDecodingTest {
    companion object {
        // "こんにちは" ("hello") encoded as Shift-JIS. These bytes are not valid UTF-8:
        // 0x82 is a UTF-8 continuation byte and cannot lead a sequence.
        private val shiftJisLyricSmf =
            intArrayOf(
                0x4D, 0x54, 0x68, 0x64, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x01, 0x00, 0x60,
                0x4D, 0x54, 0x72, 0x6B, 0x00, 0x00, 0x00, 0x12,
                0x00, 0xFF, 0x05, 0x0A, 0x82, 0xB1, 0x82, 0xF1, 0x82, 0xC9, 0x82, 0xBF, 0x82, 0xCD,
                0x00, 0xFF, 0x2F, 0x00,
            ).map { it.toByte() }.toByteArray()

        // Same structure, but with an ASCII lyric ("Hi") that is also valid UTF-8.
        private val asciiLyricSmf =
            intArrayOf(
                0x4D, 0x54, 0x68, 0x64, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x01, 0x00, 0x60,
                0x4D, 0x54, 0x72, 0x6B, 0x00, 0x00, 0x00, 0x0A,
                0x00, 0xFF, 0x05, 0x02, 0x48, 0x69,
                0x00, 0xFF, 0x2F, 0x00,
            ).map { it.toByte() }.toByteArray()
    }

    @Test
    fun testShiftJisLyricFallsBackFromUtf8() {
        val smf = StandardMidiFileReader().readByteArray(shiftJisLyricSmf)
        val lyric = smf.tracks[0].events.filterIsInstance<MetaEvent.Lyric>().single()
        assertEquals("こんにちは", lyric.text)
    }

    @Test
    fun testAsciiLyricDecodesAsUtf8() {
        val smf = StandardMidiFileReader().readByteArray(asciiLyricSmf)
        val lyric = smf.tracks[0].events.filterIsInstance<MetaEvent.Lyric>().single()
        assertEquals("Hi", lyric.text)
    }
}
