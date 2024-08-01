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
import kotlin.time.Duration

/**
 * An [Arc] that has time-based information.
 *
 * @property startTime The time at which the arc starts.
 * @property endTime The time at which the arc ends.
 */
public data class TimedArc(
    public override val noteOn: NoteEvent.NoteOn,
    public override val noteOff: NoteEvent.NoteOff,
    public val startTime: Duration,
    public val endTime: Duration,
) : Arc(noteOn, noteOff) {
    /**
     * The duration of the arc.
     */
    public val duration: Duration get() = endTime - startTime

    /**
     * Calculates the progress of the arc at a given time.
     *
     * @param time The duration to calculate the progress for.
     * @return The progress of the arc at the given time.
     */
    public fun calculateProgress(time: Duration): Double = (1.0 - (endTime - time) / duration).coerceIn(0.0..1.0)

    public companion object {
        /**
         * Converts a list of [NoteEvent] objects into a list of [TimedArc] objects.
         *
         * @param sequence The sequence to use for time-based information.
         * @param noteEvents The list of [NoteEvent] objects to convert.
         * @return The list of [TimedArc] objects generated from the [noteEvents].
         */
        public fun fromNoteEvents(
            sequence: TimeBasedSequence,
            noteEvents: List<NoteEvent>,
        ): List<TimedArc> = fromNoteEvents(noteEvents).toTimedArcs(sequence)
    }
}
