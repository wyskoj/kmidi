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

import org.wysko.kmidi.midi.StandardMidiFile.Header
import org.wysko.kmidi.midi.event.Event
import org.wysko.kmidi.midi.event.MetaEvent
import org.wysko.kmidi.midi.event.MetaEvent.EndOfTrack

private const val MAX_TPQ = 0b0111_1111_1111_1111

/**
 * A Standard MIDI file (SMF) is a file format that specifies how MIDI information is stored. It consists of a header
 * chunk followed by one or more track chunks. Each track chunk contains a sequence of MIDI events.
 *
 * @property header The header of the file.
 * @property tracks The tracks of the file.
 * @throws IllegalArgumentException If the length of [tracks] does not match the number specified in the
 * [header][Header.trackCount].
 */
public data class StandardMidiFile(
    val header: Header,
    val tracks: List<Track>
) {
    init {
        check(tracks.size == header.trackCount.toInt()) {
            "Invalid track count: header specifies ${header.trackCount} tracks, but there are ${tracks.size} tracks"
        }
    }

    /**
     * The header of a Standard MIDI file.
     *
     * @property chunkType The chunk type, which is always "MThd".
     * @property format The format the file is encoded in.
     * @property trackCount The number of tracks in the file.
     * @property division The time division of the file.
     * @throws IllegalArgumentException If [chunkType] is not "MThd".
     */
    public data class Header(
        val chunkType: String,
        val format: Format,
        val trackCount: Short,
        val division: Division
    ) {
        init {
            require(chunkType == "MThd") {
                "Invalid header chunk type: $chunkType"
            }
        }

        /**
         * Specifies the overall organization of a [StandardMidiFile].
         */
        public enum class Format(private val value: Int) {
            /**
             * The file contains a single, multichannel track.
             */
            Format0(0),

            /**
             * The file contains one or more simultaneous tracks (or MIDI outputs) of a sequence.
             */
            Format1(1),

            /**
             * The file contains one or more sequentially independent single-track patterns.
             */
            Format2(2);

            internal companion object {
                fun fromValue(value: Short): Format = entries.firstOrNull {
                    it.value == value.toInt()
                } ?: throw IllegalArgumentException("Invalid format value: $value")
            }
        }

        /**
         * The time division of a [StandardMidiFile].
         */
        public sealed class Division {
            /**
             * Metrical time specifies the number of ticks (i.e., MIDI clocks) that elapse per quarter note.
             *
             * @property ticksPerQuarterNote The number of ticks (i.e., MIDI clocks) that elapse per quarter note.
             * @throws IllegalArgumentException If [ticksPerQuarterNote] can't be represented in 15 bits (SMF
             * specification).
             */
            public data class MetricalTime(
                val ticksPerQuarterNote: Short
            ) : Division() {
                init {
                    require(ticksPerQuarterNote in 0..MAX_TPQ) {
                        "Invalid ticks per quarter note: $ticksPerQuarterNote"
                    }
                }
            }

            /**
             * The time division is specified in terms of an SMPTE timecode.
             *
             * @property framesPerSecond The number of frames per second.
             * @property ticksPerFrame The number of ticks (i.e., MIDI clocks) per frame.
             */
            public data class TimecodeBasedTime(
                val framesPerSecond: Short,
                val ticksPerFrame: Short
            ) : Division() {
                @Suppress("MagicNumber")
                private val validFramesPerSecond = setOf(-24, -25, -29, -30).map { it.toShort() }.toSet()

                init {
                    require(framesPerSecond in validFramesPerSecond) {
                        "Invalid frames per second: $framesPerSecond"
                    }
                }
            }
        }
    }

    /**
     * A track of a [StandardMidiFile]. Tracks contain a sequence of MIDI events. The last event in a track is always
     * an [EndOfTrack] event.
     *
     * @property events The events in the track.
     */
    public data class Track(val events: List<Event>) {
        /**
         * The name of the track, if it has one.
         *
         * It is defined as the first [MetaEvent.SequenceTrackName] event in the track, if there is one.
         *
         * @see MetaEvent.SequenceTrackName
         */
        val name: String? = events.filterIsInstance<MetaEvent.SequenceTrackName>().firstOrNull()?.text
    }
}
