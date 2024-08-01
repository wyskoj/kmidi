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
 * An occurrence that happens at a specified [tick].
 *
 * @property tick The time at which this event occurs, expressed in MIDI clocks, defined by the file's
 * [Division][StandardMidiFile.Header.Division].
 */
public sealed class Event(
    public open val tick: Int,
)
