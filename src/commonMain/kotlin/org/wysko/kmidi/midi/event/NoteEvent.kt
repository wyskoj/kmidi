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

/**
 * A [NoteOn] or a [NoteOff] event.
 *
 * @property note The key (note) number.
 */
public sealed class NoteEvent(
    override val time: Int,
    override val channel: Byte,
    public open val note: Byte
) : MidiEvent(time, channel) {

    /**
     * Signals a note should begin playing.
     *
     * If a MIDI file contains a NoteOn event with a velocity of 0, it should be treated as a NoteOff event, per the
     * MIDI specification. This library converts such events to NoteOff events and throws an exception if a NoteOn
     * event with a velocity of 0 is encountered.
     *
     * @property velocity The non-zero velocity of the note.
     * @throws IllegalArgumentException If [velocity] is not greater than 0.
     */
    public data class NoteOn(
        override val time: Int,
        override val channel: Byte,
        override val note: Byte,
        val velocity: Byte
    ) : NoteEvent(time, channel, note) {
        init {
            require(velocity > 0.toByte()) { "Velocity must be greater than 0." }
        }
    }

    /**
     * Signals that a note should stop playing.
     */
    public data class NoteOff(
        override val time: Int,
        override val channel: Byte,
        override val note: Byte
    ) : NoteEvent(time, channel, note)
}
