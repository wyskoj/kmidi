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

import org.wysko.kmidi.midi.StandardMidiFile.Header.Division.MetricalTime
import org.wysko.kmidi.midi.StandardMidiFile.Header.Division.TimecodeBasedTime
import org.wysko.kmidi.midi.event.ChannelPressureEvent
import org.wysko.kmidi.midi.event.ControlChangeEvent
import org.wysko.kmidi.midi.event.MetaEvent
import org.wysko.kmidi.midi.event.MidiConstants.ChannelVoiceMessages.CHANNEL_PRESSURE
import org.wysko.kmidi.midi.event.MidiConstants.ChannelVoiceMessages.CONTROL_CHANGE
import org.wysko.kmidi.midi.event.MidiConstants.ChannelVoiceMessages.NOTE_OFF_EVENT
import org.wysko.kmidi.midi.event.MidiConstants.ChannelVoiceMessages.NOTE_ON_EVENT
import org.wysko.kmidi.midi.event.MidiConstants.ChannelVoiceMessages.PITCH_WHEEL_CHANGE
import org.wysko.kmidi.midi.event.MidiConstants.ChannelVoiceMessages.POLYPHONIC_KEY_PRESSURE
import org.wysko.kmidi.midi.event.MidiConstants.ChannelVoiceMessages.PROGRAM_CHANGE
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.kmidi.midi.event.PitchWheelChangeEvent
import org.wysko.kmidi.midi.event.PolyphonicKeyPressureEvent
import org.wysko.kmidi.midi.event.ProgramEvent
import org.wysko.kmidi.midi.event.SysexEvent
import org.wysko.kmidi.util.shl
import org.wysko.kmidi.util.shr
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * Writes [StandardMidiFile] objects to bytes, as defined by the Standard MIDI File specification 1.0.
 */
@Suppress("MagicNumber")
public class StandardMidiFileWriter {
    /**
     * Writes a [StandardMidiFile] to a byte array.
     *
     * @param smf The [StandardMidiFile] to write.
     * @return The byte array representing the [StandardMidiFile].
     */
    public fun write(smf: StandardMidiFile): ByteArray =
        buildList {
            addAll(writeHeader(smf))
            smf.tracks.forEach { addAll(writeTrack(it)) }
        }.toByteArray()

    private fun writeHeader(smf: StandardMidiFile): List<Byte> =
        buildChunk(
            smf.header.chunkType,
            buildList {
                addAll(
                    short16ToBytes(
                        when (smf.header.format) {
                            StandardMidiFile.Header.Format.Format0 -> 0
                            StandardMidiFile.Header.Format.Format1 -> 1
                            StandardMidiFile.Header.Format.Format2 -> 2
                        },
                    ),
                )

                addAll(short16ToBytes(smf.header.trackCount))
                with(smf.header.division) {
                    addAll(
                        when (this) {
                            is MetricalTime -> short16ToBytes(ticksPerQuarterNote)
                            is TimecodeBasedTime -> short16ToBytes((framesPerSecond shl 8) and ticksPerFrame)
                        },
                    )
                }
            },
        )

    private fun writeTrack(track: StandardMidiFile.Track): List<Byte> =
        buildChunk(
            "MTrk",
            buildList {
                var runningStatus: Byte = 0
                var lastTick = 0

                fun writeWithRunningStatus(
                    prefix: Byte,
                    writeEventData: () -> Unit,
                ) {
                    if (prefix == runningStatus) {
                        writeEventData() // Running status
                    } else {
                        add(prefix)
                        writeEventData()
                        runningStatus = prefix
                    }
                }

                for (event in track.events) {
                    addAll(intToVlq(event.tick - lastTick)) // Delta-time

                    when (event) {
                        is MetaEvent -> {
                            addAll(writeMetaEvent(event))
                            runningStatus = 0
                        }

                        is SysexEvent -> {
                            addAll(writeSysexEvent(event))
                            runningStatus = 0
                        }

                        is ChannelPressureEvent ->
                            writeWithRunningStatus(
                                CHANNEL_PRESSURE or event.channel,
                            ) { add(event.pressure) }

                        is ControlChangeEvent ->
                            writeWithRunningStatus(CONTROL_CHANGE or event.channel) {
                                add(event.controller)
                                add(event.value)
                            }

                        is NoteEvent.NoteOff ->
                            writeWithRunningStatus(NOTE_OFF_EVENT or event.channel) {
                                add(event.note)
                                add(event.velocity)
                            }

                        is NoteEvent.NoteOn ->
                            writeWithRunningStatus(NOTE_ON_EVENT or event.channel) {
                                add(event.note)
                                add(event.velocity)
                            }

                        is PitchWheelChangeEvent ->
                            writeWithRunningStatus(PITCH_WHEEL_CHANGE or event.channel) {
                                add((event.value and 0x7F).toByte())
                                add(((event.value shr 7) and 0x7F).toByte())
                            }

                        is PolyphonicKeyPressureEvent ->
                            writeWithRunningStatus(POLYPHONIC_KEY_PRESSURE or event.channel) {
                                add(event.note)
                                add(event.pressure)
                            }

                        is ProgramEvent ->
                            writeWithRunningStatus(
                                PROGRAM_CHANGE or event.channel,
                            ) { add(event.program) }

                        else -> Unit
                    }

                    lastTick = event.tick
                }
            },
        )

    private fun writeSysexEvent(event: SysexEvent): List<Byte> =
        buildList {
            add(0xF0.toByte())
            addAll(intToVlq(event.data.size))
            addAll(event.data.toList())
        }

    private fun writeMetaEvent(event: MetaEvent): List<Byte> {
        val eventData =
            buildList {
                fun writeTextualEvent(event: MetaEvent.Textual) = addAll(event.text.toByteArray().toList())
                when (event) {
                    is MetaEvent.Textual -> writeTextualEvent(event)
                    is MetaEvent.ChannelPrefix -> add(event.channel)
                    is MetaEvent.EndOfTrack -> Unit
                    is MetaEvent.KeySignature -> {
                        add(event.key.value)
                        add(event.scale.value)
                    }

                    is MetaEvent.SequenceNumber -> addAll(short16ToBytes(event.number))
                    is MetaEvent.SequencerSpecific -> {
                        addAll(intToVlq(event.data.size))
                        addAll(event.data.toList())
                    }

                    is MetaEvent.SetTempo -> addAll(int24ToBytes(event.tempo))
                    is MetaEvent.SmpteOffset -> {
                        add(event.timecode.hours)
                        add(event.timecode.minutes)
                        add(event.timecode.seconds)
                        add(event.timecode.frames)
                        add(event.timecode.subFrames)
                    }

                    is MetaEvent.TimeSignature -> {
                        add(event.numerator)
                        add(event.denominator)
                        add(event.clocksInMetronomeClick)
                        add(event.thirtySecondNotesInMidiQuarterNote)
                    }

                    is MetaEvent.Unknown -> {
                        addAll(intToVlq(event.data.size))
                        addAll(event.data.toList())
                    }

                    else -> Unit
                }
            }

        return listOf(0xFF.toByte(), event.type) + intToVlq(eventData.size) + eventData
    }

    private fun buildChunk(
        chunkType: String,
        data: List<Byte>,
    ): List<Byte> = chunkType.toByteArray().toList() + int32ToBytes(data.size) + data

    private fun int24ToBytes(value: Int): List<Byte> {
        val bytes = mutableListOf<Byte>()
        bytes += (value shr 16).toByte()
        bytes += (value shr 8).toByte()
        bytes += value.toByte()
        return bytes
    }

    private fun int32ToBytes(value: Int): List<Byte> {
        val bytes = mutableListOf<Byte>()
        bytes += (value shr 24).toByte()
        bytes += (value shr 16).toByte()
        bytes += (value shr 8).toByte()
        bytes += value.toByte()
        return bytes
    }

    private fun short16ToBytes(value: Short): List<Byte> {
        val bytes = mutableListOf<Byte>()
        bytes += (value shr 8).toByte()
        bytes += value.toByte()
        return bytes
    }

    private fun intToVlq(value: Int): List<Byte> {
        var buffer = value and 0x7F
        var shiftValue = value ushr 7
        val result = mutableListOf<Byte>()

        while (shiftValue > 0) {
            buffer = (buffer shl 8) or 0x80
            buffer += shiftValue and 0x7F
            shiftValue = shiftValue ushr 7
        }

        while (true) {
            result.add((buffer and 0xFF).toByte())
            if (buffer and 0x80 != 0) {
                buffer = buffer ushr 8
            } else {
                break
            }
        }

        return result
    }
}
