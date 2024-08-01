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

package org.wysko.kmidi.midi.reader

import org.wysko.kmidi.midi.event.MetaEvent
import kotlin.reflect.KClass

/**
 * The exception that is thrown when an unexpected situation occurs in the process of reading a standard MIDI file.
 */
public sealed class StandardMidiFileReadingException(
    override val message: String,
) : Exception(message)

/**
 * The exception that is thrown when a file does not contain a valid SMF header.
 *
 * @param type The type of invalid header error.
 */
public class InvalidHeaderException(
    type: HeaderExceptionType,
) : StandardMidiFileReadingException(type.message) {
    /**
     * The type of invalid header error.
     *
     * @property message The message of the error.
     */
    public sealed class HeaderExceptionType(
        public val message: String,
    ) {
        /**
         * The file does not contain the SMF header chunk.
         */
        public data object MissingHeader : HeaderExceptionType("The file does not contain a header")

        /**
         * The header is a non-standard length.
         *
         * @param length The length of the header.
         */
        public data class InvalidHeaderLength(
            val length: Int,
        ) : HeaderExceptionType("Header length $length is not at least 6 bytes long")

        /**
         * The file is a non-standard format.
         *
         * @param format The format of the file.
         */
        public data class InvalidFormat(
            val format: Short,
        ) : HeaderExceptionType("Invalid format: $format")
    }
}

/**
 * The exception that is thrown when the end of a file is reached unexpectedly.
 */
public class UnexpectedEndOfFileException : StandardMidiFileReadingException("Unexpected end of file")

/**
 * The exception that is thrown when a meta-event is incomplete.
 *
 * A meta-event is incomplete if it does not at least meet the length as defined in the SMF Specification 1.0.
 *
 * @property metaEventClass The class of the incomplete meta-event.
 */
public class IncompleteMetaEventException(
    public val metaEventClass: KClass<out MetaEvent>,
) : StandardMidiFileReadingException("Incomplete meta event")
