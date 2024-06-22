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

package org.wysko.kmidi.midi

import org.wysko.kmidi.ArrayInputStream
import org.wysko.kmidi.SmpteTimecode
import org.wysko.kmidi.midi.event.ChannelPressureEvent
import org.wysko.kmidi.midi.event.ControlChangeEvent
import org.wysko.kmidi.midi.event.Event
import org.wysko.kmidi.midi.event.MetaEvent
import org.wysko.kmidi.midi.event.MetaEvent.KeySignature.Scale
import org.wysko.kmidi.midi.event.MidiConstants.ChannelVoiceMessages
import org.wysko.kmidi.midi.event.MidiConstants.MetaEvents
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.kmidi.midi.event.PitchWheelChangeEvent
import org.wysko.kmidi.midi.event.PolyphonicKeyPressureEvent
import org.wysko.kmidi.midi.event.ProgramEvent
import org.wysko.kmidi.midi.event.SysexEvent
import org.wysko.kmidi.util.shl
import org.wysko.kmidi.util.shr
import kotlin.experimental.and
import kotlin.experimental.or

private const val CHUNK_TYPE_LENGTH = 4
private const val SMF_HEADER_LENGTH = 6
private const val CHANNEL_MASK: Byte = 0b0000_1111
private const val IS_STATUS_MASK: Byte = 0b1000_0000.toByte()
private const val STATUS_MASK: Byte = 0b1111_0000.toByte()

/**
 * Parses a Standard MIDI file.
 */
@Suppress("CyclomaticComplexMethod", "LongMethod")
public object StandardMidiFileReader {

    /**
     * Parses a [ByteArray] of a Standard MIDI file.
     *
     * @param bytes The bytes of the file.
     * @return A [StandardMidiFile] that represents the contents of the input.
     */
    public fun readByteArray(bytes: ByteArray): StandardMidiFile {
        val stream = ArrayInputStream(bytes)
        val header = readHeader(stream)
        val tracks = readRemainingChunks(stream, header)
        return StandardMidiFile(header, tracks)
    }

    private fun readHeader(stream: ArrayInputStream): StandardMidiFile.Header {
        // Read the chunk type
        val chunkType = stream.readNBytes(CHUNK_TYPE_LENGTH).decodeToString()
        require(chunkType == "MThd") {
            "Invalid header chunk type: $chunkType"
        }

        // Read the length
        val length = stream.readDWord()
        require(length >= SMF_HEADER_LENGTH) {
            "Invalid header length: $length"
        }

        // Read the format
        val format = stream.readWord()
        require(format in 0..2) {
            "Invalid SMF format value: $format"
        }

        val trackCount = stream.readWord()

        // Division
        val timeDivision = stream.readWord()
        val division = if (timeDivision and 0x8000.toShort() == 0.toShort()) {
            StandardMidiFile.Header.Division.MetricalTime(
                timeDivision
            )
        } else {
            StandardMidiFile.Header.Division.TimecodeBasedTime(
                framesPerSecond = (timeDivision and 0x7F00.toShort()) shr 8,
                ticksPerFrame = timeDivision and 0x00FF.toShort()
            )
        }

        // Skip any remaining bytes (forwards compatibility)
        stream.skip(length - SMF_HEADER_LENGTH)

        return StandardMidiFile.Header(
            chunkType = chunkType,
            format = StandardMidiFile.Header.Format.fromValue(format),
            trackCount = trackCount,
            division = division
        )
    }

    private fun readRemainingChunks(
        stream: ArrayInputStream,
        header: StandardMidiFile.Header
    ): List<StandardMidiFile.Track> {
        val tracks = mutableListOf<StandardMidiFile.Track>()

        repeat(header.trackCount.toInt()) {
            val trackType = stream.readNBytes(CHUNK_TYPE_LENGTH).decodeToString()
            val trackLength = stream.readDWord()

            if (trackType != "MTrk") {
                // Skip the chunk, as it is not a track
                stream.skip(trackLength)
            } else {
                var time = 0
                var prefix: Byte = 0
                var bytesRead = 0
                val events = mutableListOf<Event>()

                while (bytesRead < trackLength) {
                    val startingPosition = stream.position
                    var data1: Byte = -1
                    var data2: Byte

                    // Read the delta time
                    val (deltaTime, _) = stream.readVlq()
                    time += deltaTime

                    // Read the status byte
                    val statusByte = stream.read()
                    if (statusByte and IS_STATUS_MASK != 0.toByte()) {
                        prefix = statusByte
                    } else {
                        data1 = statusByte
                    }

                    // Separate channel and prefix
                    val status = prefix and STATUS_MASK
                    val channel = prefix and CHANNEL_MASK

                    when {
                        status == ChannelVoiceMessages.NOTE_OFF_EVENT -> {
                            data1 = readDataByte(data1, stream)
                            stream.read() // Skip the velocity
                            events += NoteEvent.NoteOff(time, channel, data1)
                        }

                        status == ChannelVoiceMessages.NOTE_ON_EVENT -> {
                            val bytes = readTwoDataBytes(data1, stream)
                            data1 = bytes.first
                            data2 = bytes.second
                            events += NoteEvent.NoteOn(time, channel, note = data1, velocity = data2)
                        }

                        status == ChannelVoiceMessages.POLYPHONIC_KEY_PRESSURE -> {
                            val bytes = readTwoDataBytes(data1, stream)
                            data1 = bytes.first
                            data2 = bytes.second
                            events += PolyphonicKeyPressureEvent(time, channel, note = data1, pressure = data2)
                        }

                        status == ChannelVoiceMessages.CONTROL_CHANGE -> {
                            val bytes = readTwoDataBytes(data1, stream)
                            data1 = bytes.first
                            data2 = bytes.second
                            events += ControlChangeEvent(time, channel, controller = data1, value = data2)
                        }

                        status == ChannelVoiceMessages.PROGRAM_CHANGE -> {
                            data1 = readDataByte(data1, stream)
                            events += ProgramEvent(time, channel, program = data1)
                        }

                        status == ChannelVoiceMessages.CHANNEL_PRESSURE -> {
                            data1 = readDataByte(data1, stream)
                            events += ChannelPressureEvent(time, channel, pressure = data1)
                        }

                        status == ChannelVoiceMessages.PITCH_WHEEL_CHANGE -> {
                            val bytes = readTwoDataBytes(data1, stream)
                            data1 = bytes.first
                            data2 = bytes.second
                            events += PitchWheelChangeEvent(time, channel, value = ((data2 shl 7) and data1).toShort())
                        }

                        status == 0xF0.toByte() && (prefix == 0xF0.toByte() || prefix == 0xF7.toByte()) -> {
                            // Read the length of the system exclusive message
                            val (length, _) = stream.readVlq()

                            // Read the system exclusive message
                            val message = stream.readNBytes(length)

                            events += SysexEvent(time, message)
                        }

                        status == 0xF0.toByte() && prefix == 0xFF.toByte() -> {
                            val metaType = stream.read()
                            val metaEvent = readMetaEvent(stream, time, metaType)
                            events += metaEvent
                        }
                    }

                    bytesRead += stream.position - startingPosition
                }

                tracks += StandardMidiFile.Track(events)
            }
        }

        return tracks
    }

    private fun readTwoDataBytes(data1: Byte, stream: ArrayInputStream): Pair<Byte, Byte> =
        if (data1 == (-1).toByte()) {
            Pair(stream.read(), stream.read())
        } else {
            Pair(data1, stream.read())
        }

    private fun readDataByte(data1: Byte, stream: ArrayInputStream): Byte =
        if (data1 == (-1).toByte()) {
            stream.read()
        } else {
            data1
        }

    private fun readMetaEvent(stream: ArrayInputStream, time: Int, metaType: Byte): Event = when (metaType) {
        MetaEvents.SEQUENCE_NUMBER -> {
            val (_, _) = stream.readVlq() // Should always be 2
            val high = stream.read()
            val low = stream.read()

            @Suppress("MagicNumber")
            val sequenceNumber = (high shl 8).toShort() or low.toShort()

            MetaEvent.SequenceNumber(sequenceNumber)
        }

        MetaEvents.TEXT_EVENT -> {
            val (length, _) = stream.readVlq()
            val text = stream.readNBytes(length).decodeToString()
            MetaEvent.Text(time, text)
        }

        MetaEvents.COPYRIGHT_NOTICE -> {
            val (length, _) = stream.readVlq()
            val text = stream.readNBytes(length).decodeToString()
            MetaEvent.CopyrightNotice(text)
        }

        MetaEvents.SEQUENCE_TRACK_NAME -> {
            val (length, _) = stream.readVlq()
            val text = stream.readNBytes(length).decodeToString()
            MetaEvent.SequenceTrackName(text)
        }

        MetaEvents.INSTRUMENT_NAME -> {
            val (length, _) = stream.readVlq()
            val text = stream.readNBytes(length).decodeToString()
            MetaEvent.InstrumentName(text)
        }

        MetaEvents.LYRIC -> {
            val (length, _) = stream.readVlq()
            val text = stream.readNBytes(length).decodeToString()
            MetaEvent.Lyric(time, text)
        }

        MetaEvents.MARKER -> {
            val (length, _) = stream.readVlq()
            val text = stream.readNBytes(length).decodeToString()
            MetaEvent.Marker(time, text)
        }

        MetaEvents.CUE_POINT -> {
            val (length, _) = stream.readVlq()
            val text = stream.readNBytes(length).decodeToString()
            MetaEvent.CuePoint(time, text)
        }

        MetaEvents.MIDI_CHANNEL_PREFIX -> {
            val (_, _) = stream.readVlq()
            val channel = stream.read()
            MetaEvent.ChannelPrefix(time, channel)
        }

        MetaEvents.END_OF_TRACK -> {
            val (_, _) = stream.readVlq()
            MetaEvent.EndOfTrack(time)
        }

        MetaEvents.TEMPO -> {
            val (_, _) = stream.readVlq()
            val tempo = stream.read24BitInt()
            MetaEvent.SetTempo(time, tempo)
        }

        MetaEvents.SMPTE_OFFSET -> {
            val (_, _) = stream.readVlq()
            val hour = stream.read()
            val minute = stream.read()
            val second = stream.read()
            val frame = stream.read()
            val subFrame = stream.read()
            val timecode = SmpteTimecode(hour, minute, second, frame, subFrame)
            MetaEvent.SmpteOffset(timecode)
        }

        MetaEvents.KEY_SIGNATURE -> {
            val (_, _) = stream.readVlq()
            val key = stream.read()
            val scale = stream.read()
            MetaEvent.KeySignature(
                time,
                MetaEvent.KeySignature.Key.fromValue(key),
                Scale.fromValue(scale)
            )
        }

        MetaEvents.TIME_SIGNATURE -> {
            val (_, _) = stream.readVlq()
            val numerator = stream.read()
            val denominator = stream.read()
            val clocksInMetronomeClick = stream.read()
            val thirtySecondNotesInMidiQuarterNote = stream.read()
            MetaEvent.TimeSignature(
                time,
                numerator,
                denominator,
                clocksInMetronomeClick,
                thirtySecondNotesInMidiQuarterNote
            )
        }

        MetaEvents.SEQUENCER_SPECIFIC_EVENT -> {
            val (length, _) = stream.readVlq()
            val data = stream.readNBytes(length)
            MetaEvent.SequencerSpecific(time, data)
        }

        else -> {
            val (length, _) = stream.readVlq()
            val bytes = stream.readNBytes(length)
            MetaEvent.Unknown(time, metaType, bytes)
        }
    }
}
