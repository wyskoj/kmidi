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

import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.kmidi.midi.event.NoteEvent.NoteOff
import org.wysko.kmidi.midi.event.NoteEvent.NoteOn

private const val MIDI_NOTES_RANGE = 128

/**
 * An Arc is the pair of a [NoteOn] and a [NoteOff] event.
 *
 * @property noteOn The [NoteOn] event.
 * @property noteOff The [NoteOff] event.
 */
public open class Arc(
    internal open val noteOn: NoteOn,
    internal open val noteOff: NoteOff,
) {
    /**
     * The tick at which the arc starts.
     */
    public val start: Int get() = noteOn.tick

    /**
     * The tick at which the arc ends.
     */
    public val end: Int get() = noteOff.tick

    /**
     * The note value of the arc.
     */
    public val note: Byte get() = noteOn.note

    /**
     * The channel of the arc.
     */
    public val channel: Byte get() = noteOn.channel

    /**
     * The velocity of the arc.
     */
    public val velocity: Byte get() = noteOn.velocity

    public companion object {
        /**
         * Converts a list of [Arc] objects into a list of [TimedArc] objects.
         * The [TimedArc] objects will have their start and end times calculated based on the [sequence].
         *
         * @param sequence The [TimeBasedSequence] to use for calculating the start and end times.
         * @return The list of [TimedArc] objects generated from the [Arc] objects.
         */
        public fun List<Arc>.toTimedArcs(sequence: TimeBasedSequence): List<TimedArc> =
            map { arc ->
                TimedArc(
                    noteOn = arc.noteOn,
                    noteOff = arc.noteOff,
                    startTime = sequence.getTimeAtTick(arc.noteOn.tick),
                    endTime = sequence.getTimeAtTick(arc.noteOff.tick),
                )
            }

        /**
         * Converts a list of [NoteEvent] objects into a list of [Arc] objects.
         *
         * @param noteEvents The list of [NoteEvent] objects to convert.
         * @return The list of [Arc] objects generated from the [noteEvents].
         */
        public fun fromNoteEvents(noteEvents: List<NoteEvent>): List<Arc> {
            val arcs = mutableListOf<Arc>()
            val onEvents = arrayOfNulls<NoteOn>(MIDI_NOTES_RANGE)

            noteEvents.forEach { noteEvent ->
                val noteInt = noteEvent.note.toInt()

                when (noteEvent) {
                    is NoteOn ->
                        // If the same note starts again while it is playing, ignore this new NoteOn event
                        if (onEvents[noteInt] == null) {
                            onEvents[noteInt] = noteEvent
                        }

                    is NoteOff ->
                        onEvents[noteInt]?.let {
                            arcs.add(
                                Arc(it, noteEvent),
                            )
                            onEvents[noteInt] = null
                        }
                }
            }

            // Remove exact duplicates
            return arcs.distinct()
        }
    }
}
