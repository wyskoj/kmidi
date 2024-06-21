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

package org.wysko.kmidi.midi.event

import org.wysko.kmidi.SmpteTimecode
import org.wysko.kmidi.midi.StandardMidiFile
import org.wysko.kmidi.midi.event.MetaEvent.KeySignature.Key
import org.wysko.kmidi.midi.event.MetaEvent.KeySignature.Scale

private const val MAX_MIDI_CHANNEL = 15
private const val MICROSECONDS_PER_MINUTE = 60_000_000.0
private const val SECONDS_PER_MINUTE = 60

/**
 * An [Event] that specifies non-MIDI information useful to this format or to sequencers.
 */
sealed class MetaEvent(override val time: Int) : Event(time) {

    /**
     * Specifies the number of a sequence.
     *
     * *This event can only occur at the beginning of a track.*
     *
     * @property number The sequence number.
     */
    data class SequenceNumber(val number: Short) : MetaEvent(0)

    /**
     * Any amount of text describing anything.
     *
     * @property text The text.
     */
    data class Text(override val time: Int, val text: String) : MetaEvent(time)

    /**
     * A copyright notice. The notice should contain the characters (C), the year of
     * the copyright, and the owner of the copyright.
     *
     * *This event can only occur at the beginning of a track.*
     *
     * @property text The copyright notice.
     */
    data class CopyrightNotice(val text: String) : MetaEvent(0)

    /**
     * If in a [format 0][StandardMidiFile.Header.Format.Format0] track, or the first track in a
     * [format 1][StandardMidiFile.Header.Format.Format1] file, the name of the sequence. Otherwise, the name of the
     * track.
     *
     * *This event can only occur at the beginning of a track.*
     *
     * @property text The name of the sequence or track.
     */
    data class SequenceTrackName(val text: String) : MetaEvent(0)

    /**
     * A description of the type of instrumentation to be used in that track.
     *
     * *This event can only occur at the beginning of a track.*
     *
     * @property text The name of the instrument.
     */
    data class InstrumentName(val text: String) : MetaEvent(0)

    /**
     * A lyric to be sung. Generally, each syllable will be a separate lyric event which begins at the event's time.
     *
     * @property text The lyric text.
     */
    data class Lyric(override val time: Int, val text: String) : MetaEvent(time)

    /**
     * Normally in a [format 0][StandardMidiFile.Header.Format.Format0] track, or the first track in a
     * [format 1][StandardMidiFile.Header.Format.Format1] file. The name of that point in the sequence, such
     * as a rehearsal letter or section name (e.g., "First Verse").
     *
     * @property text The name of the marker.
     */
    data class Marker(override val time: Int, val text: String) : MetaEvent(time)

    /**
     * A description of something happening on a film or video screen or stage at that point in the musical score
     * (e.g., "car crashes into house", "curtain opens", "she slaps his face", etc.).
     *
     * @property text The cue point text.
     */
    data class CuePoint(override val time: Int, val text: String) : MetaEvent(time)

    /**
     * The MIDI channel (0-15) contained in this event may be used to associate a MIDI channel with all events
     * which follow, including [System exclusive][SysexEvent] and [meta-events][MetaEvent]. This channel is "effective"
     * until the next normal MIDI event (which contains a channel) or the next MIDI Channel Prefix meta-event. If MIDI
     * channels refer to "tracks", this message may be put into a [format 0][StandardMidiFile.Header.Format.Format0]
     * file, keeping their non-MIDI data associated with a track.
     *
     * @property channel The channel.
     */
    data class ChannelPrefix(override val time: Int, val channel: Byte) : MetaEvent(time) {
        init {
            require(channel in 0..MAX_MIDI_CHANNEL) { "Invalid channel: $channel" }
        }
    }

    /**
     * End of track.
     */
    data class EndOfTrack(override val time: Int) : MetaEvent(time)

    /**
     * Indicates a tempo change.
     *
     * @property tempo The new tempo, expressed in microseconds per MIDI quarter-note.
     */
    data class SetTempo(override val time: Int, val tempo: Int) : MetaEvent(time) {
        /** Returns this tempo's value as expressed in beats per minute. */
        val beatsPerMinute: Double = MICROSECONDS_PER_MINUTE / tempo

        /** Returns this tempo's value as expressed in seconds per beat. */
        val secondsPerBeat: Double = SECONDS_PER_MINUTE / beatsPerMinute
    }

    /**
     * Designates the SMPTE time at which the track chunk is supposed to start. In a
     * [format 1][StandardMidiFile.Header.Format.Format1] file, the SMPTE Offset must be stored with the tempo map, and
     * has no meaning in any of the other tracks.
     *
     * *This event can only occur at the beginning of a track.*
     *
     * @param timecode The [SmpteTimecode] at which the track chunk is supposed to start.
     */
    data class SmpteOffset(val timecode: SmpteTimecode) : MetaEvent(0)

    /**
     * Time signature is expressed as four numbers. The first two indicate the numerator and the denominator of the
     * time signature as it would be notated. The denominator is a negative power of two: 2 represents a quarter-note,
     * 3 represents an eighth-note, etc. The third number specifies the number of MIDI clocks in a metronome click.
     * The fourth number specifies the number of notated 32nd-notes in a MIDI quarter-note (24 MIDI clocks).
     *
     * @property numerator The numerator of the time signature, as it would be notated.
     * @property denominator The denominator of the time signature, as it would be notated, as a negative power of two.
     * @property clocksInMetronomeClick The number of MIDI clocks in a metronome click.
     * @property thirtySecondNotesInMidiQuarterNote The number of notated 32nd-notes in a MIDI quarter-note.
     */
    data class TimeSignature(
        override val time: Int,
        val numerator: Byte,
        val denominator: Byte,
        val clocksInMetronomeClick: Byte,
        val thirtySecondNotesInMidiQuarterNote: Byte
    ) : MetaEvent(time)

    /**
     * Indicates the key signature.
     *
     * @property key The [Key].
     * @property scale The [Scale].
     */
    data class KeySignature(override val time: Int, val key: Key, val scale: Scale) : MetaEvent(time) {

        /**
         * The key of the [KeySignature].
         */
        @Suppress("MagicNumber")
        enum class Key(private val value: Int) {
            /** Key of C♭. */
            CFlat(-7),

            /** Key of G♭. */
            GFlat(-6),

            /** Key of D♭. */
            DFlat(-5),

            /** Key of A♭. */
            AFlat(-4),

            /** Key of E♭. */
            EFlat(-3),

            /** Key of B♭. */
            BFlat(-2),

            /** Key of F. */
            F(-1),

            /** Key of C. */
            C(0),

            /** Key of G. */
            G(1),

            /** Key of D. */
            D(2),

            /** Key of A. */
            A(3),

            /** Key of E. */
            E(4),

            /** Key of B. */
            B(5),

            /** Key of F♯. */
            FSharp(6),

            /** Key of C♯. */
            CSharp(7);

            companion object {
                @Suppress("CyclomaticComplexMethod")
                fun fromValue(value: Byte): Key = entries.firstOrNull {
                    it.value == value.toInt()
                } ?: throw IllegalArgumentException("Invalid key value: $value")
            }
        }

        /**
         * The scale of the [KeySignature].
         */
        sealed class Scale(private val value: Int) {
            /** Major scale. */
            data object Major : Scale(0)

            /** Minor scale. */
            data object Minor : Scale(1)
        }
    }

    /**
     * Special requirements for particular sequencers may use this event type: the first byte or bytes of data is a
     * manufacturer ID (these are one byte, or if the first byte is 00, three bytes). As with MIDI System Exclusive,
     * manufacturers who define something using this meta-event should publish it so that others may be used by a
     * sequencer which elects to use this as its only file format; sequencers with their established feature-specific
     * formats should probably stick to the standard features when using this format.
     *
     * @property data The data.
     */
    data class SequencerSpecific(override val time: Int, val data: ByteArray) : MetaEvent(time) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as SequencerSpecific

            if (time != other.time) return false
            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = time
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    /**
     * A meta-event that is not recognized by this library.
     */
    data class Unknown(override val time: Int, val metaType: Byte, val data: ByteArray) : Event(time) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Unknown

            if (time != other.time) return false
            if (metaType != other.metaType) return false
            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = time
            result = 31 * result + metaType
            result = 31 * result + data.contentHashCode()
            return result
        }
    }
}