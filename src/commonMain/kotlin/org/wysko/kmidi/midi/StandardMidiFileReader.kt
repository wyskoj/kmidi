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
import org.wysko.kmidi.midi.InvalidHeaderException.HeaderExceptionType.InvalidFormat
import org.wysko.kmidi.midi.InvalidHeaderException.HeaderExceptionType.InvalidHeaderLength
import org.wysko.kmidi.midi.InvalidHeaderException.HeaderExceptionType.MissingHeader
import org.wysko.kmidi.midi.StandardMidiFileReader.Policies
import org.wysko.kmidi.midi.StandardMidiFileReader.Policies.UnexpectedEndOfFileExceptionPolicy
import org.wysko.kmidi.midi.StandardMidiFileReader.Policies.UnexpectedEndOfFileExceptionPolicy.AllowClean
import org.wysko.kmidi.midi.StandardMidiFileReader.Policies.UnexpectedEndOfFileExceptionPolicy.AllowDirty
import org.wysko.kmidi.midi.StandardMidiFileReader.Policies.UnexpectedEndOfFileExceptionPolicy.Disallow
import org.wysko.kmidi.midi.event.ChannelPressureEvent
import org.wysko.kmidi.midi.event.ControlChangeEvent
import org.wysko.kmidi.midi.event.Event
import org.wysko.kmidi.midi.event.MetaEvent
import org.wysko.kmidi.midi.event.MetaEvent.KeySignature
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

private const val META_LENGTH_TIME_SIGNATURE = 4
private const val CHUNK_TYPE_LENGTH = 4
private const val SMF_HEADER_LENGTH = 6
private const val CHANNEL_MASK: Byte = 0b0000_1111
private const val IS_STATUS_MASK: Byte = 0b1000_0000.toByte()
private const val STATUS_MASK: Byte = 0b1111_0000.toByte()
private const val SEVEN_BIT_MAX = 127
private val SEVEN_BIT_RANGE = 0..SEVEN_BIT_MAX

/**
 * Parses a Standard MIDI file.
 *
 * Some reading behavior can be controlled by specifying [policies].
 *
 * @property policies The policies to use when reading the file.
 * @see Policies
 */
@Suppress("CyclomaticComplexMethod", "LongMethod")
public class StandardMidiFileReader(
    private val policies: Policies = Policies.lenient,
) {
    /**
     * Policies that control the behavior of the reader.
     *
     * @property allowRunningStatusAcrossNonMidiEvents
     * Whether to allow running status to be used across non-MIDI events.
     * The Standard MIDI File Specification 1.0 states that "Sysex events and meta-events cancel any running status
     * which was in effect."
     * However, some MIDI files may not follow this rule.
     * When `true`, running status is preserved if a sysex or meta-event occurs.
     * Otherwise, the running status is reset and input may be garbled.
     *
     * @property allowTrackCountDiscrepancy
     * Whether to allow a discrepancy between the number of tracks specified in the header and the number of tracks
     * actually present in the file.
     * If `true` and a discrepancy is found, the reader will create empty tracks to match the number specified in the
     * header.
     *
     * @property coerceVelocityToRange
     * Whether to coerce the unsigned velocity value of a [NoteEvent.NoteOn] event to be within the range of 0 to 127.
     *
     * @property ignoreBadChannelPrefixes
     * Whether to ignore bad channel prefixes.
     * The Standard MIDI File Specification 1.0 provides a list of valid channels (0-15).
     * However, a MIDI file could contain an invalid value.
     * If `true`, the reader will ignore (throw out) bad [MetaEvent.ChannelPrefix] events.
     * Otherwise, an exception is thrown.
     *
     * @property ignoreBadKeySignatures
     * Whether to ignore bad key signatures.
     * The Standard MIDI File Specification 1.0 provides a list of valid keys and scales.
     * However, a MIDI file could contain an invalid value.
     * If `true`, the reader will ignore (throw out) bad [MetaEvent.KeySignature] events.
     * Otherwise, an exception is thrown.
     *
     * @property ignoreIncompleteMetaEvents
     * It is possible for a meta-event to be incomplete, i.e., the length of the event is less than the SMF
     * specification.
     * If `true`, meta-events that don't have enough bytes (but correctly have enough bytes to satisfy their
     * declaration) are ignored.
     * (For example, the SMF specification specifies that [MetaEvent.TimeSignature] events are four bytes long,
     * but it is possible for this meta-event to only have two bytes.)
     *
     * @property unexpectedEndOfFilePolicy How to handle an [UnexpectedEndOfFileException].
     *
     * @see UnexpectedEndOfFileExceptionPolicy
     */
    public data class Policies(
        val allowRunningStatusAcrossNonMidiEvents: Boolean,
        val allowTrackCountDiscrepancy: Boolean,
        val coerceVelocityToRange: Boolean,
        val ignoreBadChannelPrefixes: Boolean,
        val ignoreBadKeySignatures: Boolean,
        val ignoreIncompleteMetaEvents: Boolean,
        val unexpectedEndOfFilePolicy: UnexpectedEndOfFileExceptionPolicy,
    ) {
        public companion object {
            /**
             * A set of policies that are lenient and allow for some discrepancies in the file.
             */
            public val lenient: Policies =
                Policies(
                    allowRunningStatusAcrossNonMidiEvents = true,
                    allowTrackCountDiscrepancy = true,
                    coerceVelocityToRange = true,
                    ignoreBadChannelPrefixes = true,
                    ignoreBadKeySignatures = true,
                    ignoreIncompleteMetaEvents = true,
                    unexpectedEndOfFilePolicy = AllowDirty,
                )

            /**
             * A set of policies that are strict and do not allow for any discrepancies in the file.
             */
            public val strict: Policies =
                Policies(
                    allowRunningStatusAcrossNonMidiEvents = false,
                    allowTrackCountDiscrepancy = false,
                    coerceVelocityToRange = false,
                    ignoreBadChannelPrefixes = false,
                    ignoreBadKeySignatures = false,
                    ignoreIncompleteMetaEvents = false,
                    unexpectedEndOfFilePolicy = Disallow,
                )
        }

        /**
         * Policies that control the behavior of the reader when an [UnexpectedEndOfFileException] is thrown.
         */
        public sealed class UnexpectedEndOfFileExceptionPolicy {
            /**
             * Consume [UnexpectedEndOfFileException] and return all read data if the exception is thrown after an
             * event has been fully read.
             */
            public data object AllowClean : UnexpectedEndOfFileExceptionPolicy()

            /**
             * Consume [UnexpectedEndOfFileException] if the exception is thrown after an event has been partially read.
             */
            public data object AllowDirty : UnexpectedEndOfFileExceptionPolicy()

            /**
             * Do not consume [UnexpectedEndOfFileException] and throw it.
             */
            public data object Disallow : UnexpectedEndOfFileExceptionPolicy()
        }
    }

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
        val chunkType = stream.readNBytes(CHUNK_TYPE_LENGTH).decodeToString()
        if (chunkType != StandardMidiFile.Header.HEADER_MAGIC) throw InvalidHeaderException(MissingHeader)

        val length = stream.readDWord()
        if (length < SMF_HEADER_LENGTH) throw InvalidHeaderException(InvalidHeaderLength(length))

        val format = stream.readWord()
        if (format !in 0..2) throw InvalidHeaderException(InvalidFormat(format))

        val trackCount = stream.readWord()

        // Division
        val timeDivision = stream.readWord()
        val division =
            if (timeDivision and 0x8000.toShort() == 0.toShort()) {
                StandardMidiFile.Header.Division.MetricalTime(
                    timeDivision,
                )
            } else {
                StandardMidiFile.Header.Division.TimecodeBasedTime(
                    framesPerSecond = (timeDivision and 0xFF00.toShort()) shr 8,
                    ticksPerFrame = timeDivision and 0x00FF.toShort(),
                )
            }

        // Skip any remaining bytes (forwards compatibility)
        stream.skip(length - SMF_HEADER_LENGTH)

        return StandardMidiFile.Header(
            chunkType = chunkType,
            format = StandardMidiFile.Header.Format.fromValue(format),
            trackCount = trackCount,
            division = division,
        )
    }

    private fun readRemainingChunks(
        stream: ArrayInputStream,
        header: StandardMidiFile.Header,
    ): List<StandardMidiFile.Track> {
        val tracks = mutableListOf<StandardMidiFile.Track>()

        for (i in 0..<header.trackCount.toInt()) {
            var time = 0
            var prefix: Byte = 0
            var bytesRead = 0
            val events = mutableListOf<Event>()

            try {
                val trackType = stream.readNBytes(CHUNK_TYPE_LENGTH).decodeToString()
                val trackLength = stream.readDWord()

                if (trackType != "MTrk") {
                    // Skip the chunk, as it is not a track
                    stream.skip(trackLength)
                } else {
                    while (bytesRead < trackLength) {
                        // Some MIDI files have a trailing 0x00 byte, which should be ignored
                        val isTrailingNullByte = trackLength - bytesRead == 1 && stream.read() == 0.toByte()
                        // If there are no more events and policy allows, exit gracefully
                        val noMoreEventsAndPolicyAllows =
                            stream.available == 0 && policies.unexpectedEndOfFilePolicy == AllowClean

                        if (isTrailingNullByte || noMoreEventsAndPolicyAllows) {
                            break
                        }

                        val startingPosition = stream.position
                        var data1: Byte = -1
                        var data2: Byte

                        // Read the delta time
                        val (deltaTime, _) = stream.readVlq()
                        time += deltaTime

                        // Read the status byte
                        val lastPrefix = prefix
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
                                val bytes = readTwoDataBytes(data1, stream)
                                data1 = bytes.first
                                data2 = bytes.second
                                events += NoteEvent.NoteOff(time, channel, data1, data2)
                            }

                            status == ChannelVoiceMessages.NOTE_ON_EVENT -> {
                                val bytes = readTwoDataBytes(data1, stream)
                                data1 = bytes.first
                                data2 = bytes.second

                                if (policies.coerceVelocityToRange) {
                                    data2 =
                                        data2
                                            .toUByte()
                                            .toInt()
                                            .coerceIn(SEVEN_BIT_RANGE)
                                            .toByte()
                                }

                                // If the velocity is 0, it should be treated as a NoteOff event
                                if (data2 == 0.toByte()) {
                                    events += NoteEvent.NoteOff(time, channel, note = data1, velocity = 0)
                                } else {
                                    events += NoteEvent.NoteOn(time, channel, note = data1, velocity = data2)
                                }
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
                                events +=
                                    PitchWheelChangeEvent(
                                        time,
                                        channel,
                                        value = (data2.toShort() shl 7) or data1.toShort(),
                                    )
                            }

                            status == 0xF0.toByte() && (prefix == 0xF0.toByte() || prefix == 0xF7.toByte()) -> {
                                // Read the length of the system-exclusive message
                                val (length, _) = stream.readVlq()

                                // Read the system-exclusive message
                                val message = stream.readNBytes(length)

                                events += SysexEvent(time, message)

                                if (policies.allowRunningStatusAcrossNonMidiEvents) {
                                    prefix = lastPrefix
                                }
                            }

                            status == 0xF0.toByte() && prefix == 0xFF.toByte() -> {
                                val metaType = stream.read()
                                readMetaEvent(stream, time, metaType)?.let { events += it }

                                if (policies.allowRunningStatusAcrossNonMidiEvents) {
                                    prefix = lastPrefix
                                }
                            }
                        }

                        bytesRead += stream.position - startingPosition
                    }

                    tracks += StandardMidiFile.Track(events)
                }
            } catch (e: UnexpectedEndOfFileException) {
                // If the policy allows, return the tracks that have been read
                if (policies.unexpectedEndOfFilePolicy == AllowDirty) {
                    tracks += StandardMidiFile.Track(events)
                    break
                } else {
                    throw e
                }
            }
        }

        if (policies.allowTrackCountDiscrepancy) {
            // Add empty tracks to match the number specified in the header
            repeat(header.trackCount.toInt() - tracks.size) {
                tracks += StandardMidiFile.Track(emptyList())
            }
        }

        return tracks
    }

    private fun readTwoDataBytes(
        data1: Byte,
        stream: ArrayInputStream,
    ): Pair<Byte, Byte> =
        if (data1 == (-1).toByte()) {
            Pair(stream.read(), stream.read())
        } else {
            Pair(data1, stream.read())
        }

    private fun readDataByte(
        data1: Byte,
        stream: ArrayInputStream,
    ): Byte =
        if (data1 == (-1).toByte()) {
            stream.read()
        } else {
            data1
        }

    private fun readMetaEvent(
        stream: ArrayInputStream,
        time: Int,
        metaType: Byte,
    ): Event? =
        when (metaType) {
            MetaEvents.SEQUENCE_NUMBER -> {
                readSequenceNumber(stream)
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
                readMidiChannelPrefix(stream, time)
            }

            MetaEvents.END_OF_TRACK -> {
                val (length, _) = stream.readVlq()
                stream.skip(length)
                MetaEvent.EndOfTrack(time)
            }

            MetaEvents.TEMPO -> {
                readTempo(stream, time)
            }

            MetaEvents.SMPTE_OFFSET -> {
                readSmpteOffset(stream)
            }

            MetaEvents.KEY_SIGNATURE -> {
                readKeySignature(stream, time)
            }

            MetaEvents.TIME_SIGNATURE -> {
                readTimeSignature(stream, time)
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

    private fun readTimeSignature(
        stream: ArrayInputStream,
        time: Int,
    ): MetaEvent.TimeSignature? {
        val (length, _) = stream.readVlq()

        if (length < META_LENGTH_TIME_SIGNATURE) {
            if (policies.ignoreIncompleteMetaEvents) {
                return null
            } else {
                throw IncompleteMetaEventException(MetaEvent.TimeSignature::class)
            }
        }

        val numerator = stream.read()
        val denominator = stream.read()
        val clocksInMetronomeClick = stream.read()
        val thirtySecondNotesInMidiQuarterNote = stream.read()
        stream.skip(length - META_LENGTH_TIME_SIGNATURE)

        return MetaEvent.TimeSignature(
            time,
            numerator,
            denominator,
            clocksInMetronomeClick,
            thirtySecondNotesInMidiQuarterNote,
        )
    }

    private fun readKeySignature(
        stream: ArrayInputStream,
        time: Int,
    ): KeySignature? {
        val (length, _) = stream.readVlq()

        if (length < 2) {
            if (policies.ignoreIncompleteMetaEvents) {
                return null
            } else {
                throw IncompleteMetaEventException(KeySignature::class)
            }
        }

        val key = stream.read()
        val scale = stream.read()
        stream.skip(length - 2)

        return try {
            KeySignature(
                time,
                KeySignature.Key.fromValue(key),
                Scale.fromValue(scale),
            )
        } catch (e: IllegalArgumentException) {
            when {
                policies.ignoreBadKeySignatures -> null
                else -> throw e
            }
        }
    }

    private fun readSmpteOffset(stream: ArrayInputStream): MetaEvent.SmpteOffset? {
        val (length, _) = stream.readVlq()

        if (length < 5) {
            if (policies.ignoreIncompleteMetaEvents) {
                return null
            } else {
                throw IncompleteMetaEventException(MetaEvent.SmpteOffset::class)
            }
        }

        val hour = stream.read()
        val minute = stream.read()
        val second = stream.read()
        val frame = stream.read()
        val subFrame = stream.read()
        stream.skip(length - 5)

        return MetaEvent.SmpteOffset(SmpteTimecode(hour, minute, second, frame, subFrame))
    }

    private fun readTempo(
        stream: ArrayInputStream,
        time: Int,
    ): MetaEvent.SetTempo? {
        val (length, _) = stream.readVlq()

        if (length < 3) {
            if (policies.ignoreIncompleteMetaEvents) {
                return null
            } else {
                throw IncompleteMetaEventException(MetaEvent.SetTempo::class)
            }
        }

        val tempo = stream.read24BitInt()
        stream.skip(length - 3)

        return MetaEvent.SetTempo(time, tempo)
    }

    private fun readMidiChannelPrefix(
        stream: ArrayInputStream,
        time: Int,
    ): MetaEvent.ChannelPrefix? {
        val (length, _) = stream.readVlq()

        if (length < 1) {
            if (policies.ignoreIncompleteMetaEvents) {
                return null
            } else {
                throw IncompleteMetaEventException(MetaEvent.ChannelPrefix::class)
            }
        }

        val channel = stream.read()
        stream.skip(length - 1)
        return try {
            MetaEvent.ChannelPrefix(time, channel)
        } catch (e: IllegalArgumentException) {
            when {
                policies.ignoreBadChannelPrefixes -> null
                else -> throw e
            }
        }
    }

    private fun readSequenceNumber(stream: ArrayInputStream): MetaEvent.SequenceNumber? {
        val (length, _) = stream.readVlq()

        if (length < 2) {
            if (policies.ignoreIncompleteMetaEvents) {
                return null
            } else {
                throw IncompleteMetaEventException(MetaEvent.SequenceNumber::class)
            }
        }

        val high = stream.read()
        val low = stream.read()
        stream.skip(length - 2)

        return MetaEvent.SequenceNumber((high shl 8).toShort() or low.toShort())
    }
}
