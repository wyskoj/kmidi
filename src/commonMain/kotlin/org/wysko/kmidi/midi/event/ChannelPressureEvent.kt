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
 * This message is most often sent by pressing down on the key after it "bottoms
 * out". This message is different from [PolyphonicKeyPressureEvent]. Use this message
 * to send the single greatest pressure value (of all the current depressed keys).
 *
 * @property pressure The pressure value.
 */
public data class ChannelPressureEvent(
    override val tick: Int,
    override val channel: Byte,
    val pressure: Byte,
) : MidiEvent(tick, channel)
