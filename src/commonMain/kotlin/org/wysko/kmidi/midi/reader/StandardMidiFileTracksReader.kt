package org.wysko.kmidi.midi.reader

import org.wysko.kmidi.ArrayInputStream
import org.wysko.kmidi.SmpteTimecode
import org.wysko.kmidi.midi.StandardMidiFile
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
import org.wysko.kmidi.midi.reader.StandardMidiFileReader.Policies.UnexpectedEndOfFileExceptionPolicy.AllowClean
import org.wysko.kmidi.midi.reader.StandardMidiFileReader.Policies.UnexpectedEndOfFileExceptionPolicy.AllowDirty
import org.wysko.kmidi.util.shl
import kotlin.experimental.and
import kotlin.experimental.or

internal class StandardMidiFileTracksReader(
    private val policies: StandardMidiFileReader.Policies,
) {
    internal fun readRemainingChunks(
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

    private fun readDataByte(
        data1: Byte,
        stream: ArrayInputStream,
    ): Byte =
        if (data1 == (-1).toByte()) {
            stream.read()
        } else {
            data1
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
}
