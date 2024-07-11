package org.wysko.kmidi.midi.reader

import org.wysko.kmidi.ArrayInputStream
import org.wysko.kmidi.midi.StandardMidiFile
import org.wysko.kmidi.midi.event.MetaEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.kmidi.midi.reader.StandardMidiFileReader.Policies
import org.wysko.kmidi.midi.reader.StandardMidiFileReader.Policies.UnexpectedEndOfFileExceptionPolicy
import org.wysko.kmidi.midi.reader.StandardMidiFileReader.Policies.UnexpectedEndOfFileExceptionPolicy.AllowDirty
import org.wysko.kmidi.midi.reader.StandardMidiFileReader.Policies.UnexpectedEndOfFileExceptionPolicy.Disallow

internal const val META_LENGTH_TIME_SIGNATURE = 4
internal const val CHUNK_TYPE_LENGTH = 4
internal const val SMF_HEADER_LENGTH = 6
internal const val CHANNEL_MASK: Byte = 0b0000_1111
internal const val IS_STATUS_MASK: Byte = 0b1000_0000.toByte()
internal const val STATUS_MASK: Byte = 0b1111_0000.toByte()
internal const val SEVEN_BIT_MAX = 127
internal val SEVEN_BIT_RANGE = 0..SEVEN_BIT_MAX

/**
 * Parses a Standard MIDI file.
 *
 * Some reading behavior can be controlled by specifying [policies].
 *
 * @property policies The policies to use when reading the file.
 * @see Policies
 */
@Suppress("CyclomaticComplexMethod", "LongMethod")
public class StandardMidiFileReader(
    private val policies: Policies = Policies.lenient,
) {
    /**
     * Policies that control the behavior of the reader.
     *
     * @property allowRunningStatusAcrossNonMidiEvents
     * Whether to allow running status to be used across non-MIDI events.
     * The Standard MIDI File Specification 1.0 states that "Sysex events and meta-events cancel any running status
     * which was in effect."
     * However, some MIDI files may not follow this rule.
     * When `true`, running status is preserved if a sysex or meta-event occurs.
     * Otherwise, the running status is reset and input may be garbled.
     *
     * @property allowTrackCountDiscrepancy
     * Whether to allow a discrepancy between the number of tracks specified in the header and the number of tracks
     * actually present in the file.
     * If `true` and a discrepancy is found, the reader will create empty tracks to match the number specified in the
     * header.
     *
     * @property coerceVelocityToRange
     * Whether to coerce the unsigned velocity value of a [NoteEvent.NoteOn] event to be within the range of 0 to 127.
     *
     * @property ignoreBadChannelPrefixes
     * Whether to ignore bad channel prefixes.
     * The Standard MIDI File Specification 1.0 provides a list of valid channels (0-15).
     * However, a MIDI file could contain an invalid value.
     * If `true`, the reader will ignore (throw out) bad [MetaEvent.ChannelPrefix] events.
     * Otherwise, an exception is thrown.
     *
     * @property ignoreBadKeySignatures
     * Whether to ignore bad key signatures.
     * The Standard MIDI File Specification 1.0 provides a list of valid keys and scales.
     * However, a MIDI file could contain an invalid value.
     * If `true`, the reader will ignore (throw out) bad [MetaEvent.KeySignature] events.
     * Otherwise, an exception is thrown.
     *
     * @property ignoreIncompleteMetaEvents
     * It is possible for a meta-event to be incomplete, i.e., the length of the event is less than the SMF
     * specification.
     * If `true`, meta-events that don't have enough bytes (but correctly have enough bytes to satisfy their
     * declaration) are ignored.
     * (For example, the SMF specification specifies that [MetaEvent.TimeSignature] events are four bytes long,
     * but it is possible for this meta-event to only have two bytes.)
     *
     * @property unexpectedEndOfFilePolicy How to handle an [UnexpectedEndOfFileException].
     *
     * @see UnexpectedEndOfFileExceptionPolicy
     */
    public data class Policies(
        val allowRunningStatusAcrossNonMidiEvents: Boolean,
        val allowTrackCountDiscrepancy: Boolean,
        val coerceVelocityToRange: Boolean,
        val ignoreBadChannelPrefixes: Boolean,
        val ignoreBadKeySignatures: Boolean,
        val ignoreIncompleteMetaEvents: Boolean,
        val unexpectedEndOfFilePolicy: UnexpectedEndOfFileExceptionPolicy,
    ) {
        public companion object {
            /**
             * A set of policies that are lenient and allow for some discrepancies in the file.
             */
            public val lenient: Policies =
                Policies(
                    allowRunningStatusAcrossNonMidiEvents = true,
                    allowTrackCountDiscrepancy = true,
                    coerceVelocityToRange = true,
                    ignoreBadChannelPrefixes = true,
                    ignoreBadKeySignatures = true,
                    ignoreIncompleteMetaEvents = true,
                    unexpectedEndOfFilePolicy = AllowDirty,
                )

            /**
             * A set of policies that are strict and do not allow for any discrepancies in the file.
             */
            public val strict: Policies =
                Policies(
                    allowRunningStatusAcrossNonMidiEvents = false,
                    allowTrackCountDiscrepancy = false,
                    coerceVelocityToRange = false,
                    ignoreBadChannelPrefixes = false,
                    ignoreBadKeySignatures = false,
                    ignoreIncompleteMetaEvents = false,
                    unexpectedEndOfFilePolicy = Disallow,
                )
        }

        /**
         * Policies that control the behavior of the reader when an [UnexpectedEndOfFileException] is thrown.
         */
        public sealed class UnexpectedEndOfFileExceptionPolicy {
            /**
             * Consume [UnexpectedEndOfFileException] and return all read data if the exception is thrown after an
             * event has been fully read.
             */
            public data object AllowClean : UnexpectedEndOfFileExceptionPolicy()

            /**
             * Consume [UnexpectedEndOfFileException] if the exception is thrown after an event has been partially read.
             */
            public data object AllowDirty : UnexpectedEndOfFileExceptionPolicy()

            /**
             * Do not consume [UnexpectedEndOfFileException] and throw it.
             */
            public data object Disallow : UnexpectedEndOfFileExceptionPolicy()
        }
    }

    /**
     * Parses a [ByteArray] of a Standard MIDI file.
     *
     * @param bytes The bytes of the file.
     * @return A [StandardMidiFile] that represents the contents of the input.
     */
    public fun readByteArray(bytes: ByteArray): StandardMidiFile {
        val stream = ArrayInputStream(bytes)
        val header = StandardMidiFileHeaderReader(stream).readHeader()
        val tracks = StandardMidiFileTracksReader(policies).readRemainingChunks(stream, header)
        return StandardMidiFile(header, tracks)
    }
}
