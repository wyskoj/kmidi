/*
 * Copyright © 2026 Jacob Wysko
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

package org.wysko.kmidi.util

import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * Decodes a byte array from a MIDI text-carrying meta-event into a [String].
 *
 * The MIDI specification does not define a character encoding for text-carrying meta-events
 * (e.g., lyrics, track names, markers). In practice, most files use UTF-8, but some—especially
 * Japanese files—use Shift-JIS. This function attempts a strict UTF-8 decode first (one that
 * fails on invalid byte sequences, rather than silently substituting replacement characters),
 * falling back to Shift-JIS if that fails.
 */
internal fun ByteArray.decodeMetaEventText(): String =
    try {
        StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(java.nio.ByteBuffer.wrap(this))
            .toString()
    } catch (e: CharacterCodingException) {
        String(this, charset("Shift_JIS"))
    }
