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

package org.wysko.kmidi

/**
 * Representation of an SMPTE timecode, which is used in film, video, and audio production.
 *
 * @property hours The hour component of the timecode.
 * @property minutes The minute component of the timecode.
 * @property seconds The second component of the timecode.
 * @property frames The frame component of the timecode.
 * @property subFrames The fractional frame component of the timecode, in 1/100th of a frame.
 */
public data class SmpteTimecode(
    val hours: Byte,
    val minutes: Byte,
    val seconds: Byte,
    val frames: Byte,
    val subFrames: Byte,
)
