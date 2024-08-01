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

package org.wysko.kmidi.midi.analysis

import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.kmidi.midi.event.NoteEvent.NoteOff
import org.wysko.kmidi.midi.event.NoteEvent.NoteOn

/**
 * A collection of functions for analyzing polyphony in a MIDI sequence. Polyphony refers to the maximum number of
 * simultaneous notes being played at a given time.
 */
public object Polyphony {
    /**
     * Calculates the maximum polyphony from a list of note events.
     *
     * @param noteEvents The list of note events.
     * @return The maximum polyphony.
     */
    public fun calculateMaximumPolyphony(noteEvents: List<NoteEvent>): Int {
        val polyphonyIndex = buildPolyphonyIndex(noteEvents)
        return polyphonyIndex.values.maxOrNull() ?: 0
    }

    /**
     * Calculates the average polyphony from a list of note events.
     *
     * Note that this analysis method is in tick-space, not time-space.
     *
     * If it happens to be that the total number of seconds is 0, the average polyphony will be 0.
     *
     * @param noteEvents The list of note events.
     * @return The average polyphony.
     */
    public fun averagePolyphony(noteEvents: List<NoteEvent>): Double {
        val polyphonyIndex = buildPolyphonyIndex(noteEvents)
        val polyphonyTicks =
            polyphonyIndex.map { (time, polyphony) ->
                val duration = time.second - time.first
                polyphony * duration
            }
        val totalSeconds = polyphonyIndex.keys.sumOf { it.second - it.first }
        val averagePolyphony = polyphonyTicks.sum() / totalSeconds.toDouble()
        return if (averagePolyphony.isNaN()) 0.0 else averagePolyphony
    }

    private fun buildPolyphonyIndex(noteEvents: List<NoteEvent>): Map<Pair<Int, Int>, Int> {
        if (noteEvents.isEmpty()) return emptyMap()

        val events = noteEvents.sortedBy { it.tick }
        val polyphonyIndex = mutableMapOf<Pair<Int, Int>, Int>()

        var polyphony = 0
        var lastTime = events.first().tick
        for (event in events) {
            polyphonyIndex[lastTime to event.tick] = polyphony
            when (event) {
                is NoteOn -> polyphony++
                is NoteOff -> polyphony--
            }
            lastTime = event.tick
        }

        return polyphonyIndex
    }
}
