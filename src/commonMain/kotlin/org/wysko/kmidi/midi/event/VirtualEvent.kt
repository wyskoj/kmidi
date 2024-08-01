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

import org.wysko.kmidi.midi.StandardMidiFile

/**
 * A virtual event that is not an actual MIDI event, but a virtual construct for convenience.
 */
public sealed class VirtualEvent(
    override val tick: Int,
) : Event(tick) {
    public companion object {
        /**
         * Collects all [VirtualEvent]s from a [StandardMidiFile].
         *
         * @receiver The [StandardMidiFile] to collect virtual events from.
         * @return A list of all virtual events in the [StandardMidiFile].
         */
        public fun StandardMidiFile.collectVirtualEvents(): List<VirtualEvent> =
            with(tracks.flatMap { it.events }.filterIsInstance<MidiEvent>()) {
                (
                    VirtualParameterNumberChangeEvent.fromEvents(this) +
                        VirtualCompositePitchBendEvent.fromEvents(this)
                ).sortedBy { it.tick }
            }
    }
}
