package org.wysko.kmidi.midi

/**
 * The exception that is thrown when an unexpected situation occurs in the process of reading a standard MIDI file.
 */
public sealed class StandardMidiFileReadingException(override val message: String) : Exception(message)

/**
 * The exception that is thrown when a file does not contain a valid SMF header.
 *
 * @param type The type of invalid header error.
 */
public class InvalidHeaderException(type: HeaderExceptionType) :
    StandardMidiFileReadingException(type.message) {
    public sealed class HeaderExceptionType(public val message: String) {

        /**
         * The file does not contain the SMF header chunk.
         */
        public data object MissingHeader : HeaderExceptionType("The file does not contain a header")

        /**
         * The header is a non-standard length.
         */
        public data class InvalidHeaderLength(val length: Int) :
            HeaderExceptionType("Header length $length is not at least 6 bytes long")

        /**
         * The file is a non-standard format.
         */
        public data class InvalidFormat(val format: Short) : HeaderExceptionType("Invalid format: $format")
    }
}

/**
 * The exception that is thrown when the end of a file is reached unexpectedly.
 */
public class UnexpectedEndOfFileException : StandardMidiFileReadingException("Unexpected end of file")