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

package org.wysko.kmidi.midi.reader

import org.wysko.kmidi.ArrayInputStream
import org.wysko.kmidi.midi.StandardMidiFile
import org.wysko.kmidi.midi.StandardMidiFile.Header.Division.MetricalTime
import org.wysko.kmidi.midi.StandardMidiFile.Header.Division.TimecodeBasedTime
import org.wysko.kmidi.midi.reader.InvalidHeaderException.HeaderExceptionType.InvalidFormat
import org.wysko.kmidi.midi.reader.InvalidHeaderException.HeaderExceptionType.InvalidHeaderLength
import org.wysko.kmidi.midi.reader.InvalidHeaderException.HeaderExceptionType.MissingHeader
import org.wysko.kmidi.util.shr
import kotlin.experimental.and

internal class StandardMidiFileHeaderReader(
    private val stream: ArrayInputStream,
) {
    fun readHeader(): StandardMidiFile.Header {
        with(parseMidiFileHeader()) {
            validateHeader(chunkType, length, format)
            val division = determineTimeDivision(timeDivision)
            stream.skip(length - SMF_HEADER_LENGTH)
            return buildHeader(chunkType, format, trackCount, division)
        }
    }

    private fun parseMidiFileHeader(): HeaderDetails {
        val chunkType = stream.readNBytes(CHUNK_TYPE_LENGTH).decodeToString()
        val length = stream.readDWord()
        val format = stream.readWord()
        val trackCount = stream.readWord()
        val timeDivision = stream.readWord()
        return HeaderDetails(chunkType, length, format, trackCount, timeDivision)
    }

    private fun validateHeader(
        chunkType: String,
        length: Int,
        format: Short,
    ) {
        if (chunkType != StandardMidiFile.Header.HEADER_MAGIC) {
            throw InvalidHeaderException(MissingHeader)
        }

        if (length < SMF_HEADER_LENGTH) {
            throw InvalidHeaderException(InvalidHeaderLength(length))
        }

        if (format !in 0..2) {
            throw InvalidHeaderException(InvalidFormat(format))
        }
    }

    private fun determineTimeDivision(timeDivision: Short): StandardMidiFile.Header.Division =
        when {
            timeDivision and 0x8000.toShort() == 0.toShort() -> MetricalTime(timeDivision)
            else ->
                TimecodeBasedTime(
                    framesPerSecond = (timeDivision and 0xFF00.toShort()) shr 8,
                    ticksPerFrame = timeDivision and 0x00FF.toShort(),
                )
        }

    private fun buildHeader(
        chunkType: String,
        format: Short,
        trackCount: Short,
        division: StandardMidiFile.Header.Division,
    ): StandardMidiFile.Header =
        StandardMidiFile.Header(
            chunkType = chunkType,
            format = StandardMidiFile.Header.Format.fromValue(format),
            trackCount = trackCount,
            division = division,
        )
}

private data class HeaderDetails(
    val chunkType: String,
    val length: Int,
    val format: Short,
    val trackCount: Short,
    val timeDivision: Short,
)
