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
 * A MIDI system exclusive event, or "sysex" event, is a MIDI message that carries system exclusive data.
 * This data can be used to control various parameters of a synthesizer or effects unit.
 *
 * @property data The data to be sent.
 */
public data class SysexEvent(
    override val time: Int,
    val data: ByteArray
) : Event(time) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SysexEvent

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
