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

import org.wysko.kmidi.midi.StandardMidiFile.Header.Format.Format0
import org.wysko.kmidi.midi.TimeBasedSequence.Companion.toTimeBasedSequence
import org.wysko.kmidi.midi.builder.smf
import org.wysko.kmidi.midi.event.MetaEvent
import org.wysko.kmidi.midi.reader.StandardMidiFileReader
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

/**
 * Yamaha XF files keep their lyrics in an `XFKM` chunk after the tracks the header counts, where a reader that stops
 * at the last counted track never sees them.
 */
class XfKaraokeTrackTest {
    private val song =
        smf {
            division = tpq(96)
            format = Format0
            track { channel(0) { note("C4", 1.quarter) } }
        }

    private val songBytes = StandardMidiFileWriter().write(song)

    /** An XF information chunk, which comes before the karaoke chunk. */
    private val xfih = chunk("XFIH", 0x00, 0xFF, 0x01, 0x04, 'X', 'F', 'h', 'd', 0x00, 0xFF, 0x2F, 0x00)

    /** A cue point, "I^" at tick 0 and "hear/" at tick 96. */
    private val xfkm =
        chunk(
            "XFKM",
            0x00, 0xFF, 0x07, 0x02, '&', 's',
            0x00, 0xFF, 0x05, 0x02, 'I', '^',
            0x60, 0xFF, 0x05, 0x05, 'h', 'e', 'a', 'r', '/',
            0x00, 0xFF, 0x2F, 0x00,
        )

    private val karaokeTrack =
        StandardMidiFile.Track(
            listOf(
                MetaEvent.CuePoint(0, "&s"),
                MetaEvent.Lyric(0, "I^"),
                MetaEvent.Lyric(96, "hear/"),
                MetaEvent.EndOfTrack(96),
            ),
        )

    @Test
    fun `Test XF karaoke chunk is read as the karaoke track`() {
        val smf = StandardMidiFileReader().readByteArray(songBytes + xfih + xfkm)
        assertEquals(karaokeTrack, smf.xfKaraokeTrack)
        assertEquals(song.tracks, smf.tracks)
    }

    @Test
    fun `Test XF karaoke chunk is read from a stream`() {
        val smf = StandardMidiFileReader().readStream(ByteArrayInputStream(songBytes + xfih + xfkm))
        assertEquals(karaokeTrack, smf.xfKaraokeTrack)
        assertEquals(song.tracks, smf.tracks)
    }

    @Test
    fun `Test file without XF karaoke chunk has no karaoke track`() {
        assertNull(StandardMidiFileReader().readByteArray(songBytes).xfKaraokeTrack)
        assertNull(StandardMidiFileReader().readByteArray(songBytes + xfih).xfKaraokeTrack)
        assertNull(StandardMidiFileReader().readStream(ByteArrayInputStream(songBytes + xfih)).xfKaraokeTrack)
    }

    @Test
    fun `Test trailing garbage after the tracks is ignored`() {
        val garbage = byteArrayOf(0x00, 0x00, 0xFF.toByte(), 0x13, 0x37)
        assertEquals(song, StandardMidiFileReader().readByteArray(songBytes + garbage))
        assertEquals(song, StandardMidiFileReader().readStream(ByteArrayInputStream(songBytes + garbage)))
        val negativeLength = byteArrayOf('A'.code.toByte(), 'B'.code.toByte(), 'C'.code.toByte(), 'D'.code.toByte(), 0xFF.toByte(), 0, 0, 0)
        assertEquals(song, StandardMidiFileReader().readByteArray(songBytes + negativeLength))
        assertEquals(song, StandardMidiFileReader().readStream(ByteArrayInputStream(songBytes + negativeLength)))
    }

    @Test
    fun `Test XF karaoke chunk cut short keeps the lyrics before the cut`() {
        val cut = (songBytes + xfih + xfkm).let { it.copyOf(it.size - 8) }
        val smf = StandardMidiFileReader().readByteArray(cut)
        assertEquals(karaokeTrack.events.take(2), smf.xfKaraokeTrack?.events)
        assertEquals(song.tracks, smf.tracks)
    }

    @Test
    fun `Test XF karaoke events have times but don't lengthen the sequence`() {
        val karaoke = song.copy(xfKaraokeTrack = StandardMidiFile.Track(listOf(MetaEvent.Lyric(960, "late"))))
        val sequence = karaoke.toTimeBasedSequence()
        assertEquals(5.seconds, sequence.getTimeOf(karaoke.xfKaraokeTrack!!.events.single()))
        assertEquals(song.toTimeBasedSequence().duration, sequence.duration)
    }

    @Test
    fun `Test write then read XF karaoke track`() {
        val karaoke = song.copy(xfKaraokeTrack = karaokeTrack)
        val bytes = StandardMidiFileWriter().write(karaoke)
        assertEquals(karaoke, StandardMidiFileReader().readByteArray(bytes))
    }

    private fun chunk(
        type: String,
        vararg data: Any,
    ): ByteArray {
        val body = data.map { if (it is Char) it.code.toByte() else (it as Int).toByte() }
        return (type.encodeToByteArray().toList() + listOf(24, 16, 8, 0).map { (body.size shr it).toByte() } + body)
            .toByteArray()
    }
}
