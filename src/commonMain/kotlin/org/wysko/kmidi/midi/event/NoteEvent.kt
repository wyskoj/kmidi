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

package org.wysko.kmidi.midi.event

/**
 * A [NoteOn] or a [NoteOff] event.
 *
 * @property note The key (note) number.
 * @property velocity The velocity of the note.
 * In a [NoteOn] event, this is the velocity the note should be played at.
 * In a [NoteOff] event, this is the velocity the note should be released at.
 */
public sealed class NoteEvent(
    override val tick: Int,
    override val channel: Byte,
    public open val note: Byte,
    public open val velocity: Byte,
) : MidiEvent(tick, channel) {
    /**
     * Signals a note should begin playing.
     *
     * If a MIDI file contains a NoteOn event with a velocity of 0, it should be treated as a NoteOff event, per the
     * MIDI specification. This library converts such events to NoteOff events and throws an exception if a NoteOn
     * event with a velocity of 0 is encountered.
     *
     * @throws IllegalArgumentException If [velocity] is not greater than 0.
     */
    public data class NoteOn(
        override val tick: Int,
        override val channel: Byte,
        override val note: Byte,
        override val velocity: Byte,
    ) : NoteEvent(tick, channel, note, velocity) {
        init {
            require(velocity > 0.toByte()) { "Velocity must be greater than 0." }
        }
    }

    /**
     * Signals that a note should stop playing.
     */
    public data class NoteOff(
        override val tick: Int,
        override val channel: Byte,
        override val note: Byte,
        override val velocity: Byte,
    ) : NoteEvent(tick, channel, note, velocity)

    public companion object {
        /**
         * Filters a list of [NoteEvent]s to only include those with notes in [notes].
         *
         * @param notes The notes to filter by.
         * @return A list of [NoteEvent]s with notes in [notes].
         */
        public fun <T : NoteEvent> List<T>.filterByNotes(vararg notes: Byte): List<T> =
            filter { it.note in notes.toList() }
    }
}
