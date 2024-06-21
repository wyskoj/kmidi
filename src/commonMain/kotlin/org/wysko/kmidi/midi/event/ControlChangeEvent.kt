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
 * This message is sent when a controller value changes. Controllers include
 * devices such as pedals and levers. Certain controller numbers are reserved for
 * specific purposes.
 *
 * @property controller The controller number.
 * @property value The new controller value.
 */
data class ControlChangeEvent(
    override val time: Int,
    override val channel: Byte,
    val controller: Byte,
    val value: Byte
) : MidiEvent(time, channel)
