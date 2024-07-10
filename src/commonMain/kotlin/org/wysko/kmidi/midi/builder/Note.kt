package org.wysko.kmidi.midi.builder

private const val NOTES_IN_OCTAVE = 12
private const val MAX_MIDI_NOTE = 127

/**
 * Represents a MIDI note.
 *
 * @property note The MIDI note number.
 * @property velocity The velocity of the note.
 * @property duration The duration of the note, in ticks.
 */
public data class Note(
    val note: Int,
    val duration: Int,
    val velocity: Int = 100,
) {
    /**
     * @see get
     */
    public companion object {
        private val notes =
            mapOf(
                "C" to 0,
                "D" to 2,
                "E" to 4,
                "F" to 5,
                "G" to 7,
                "A" to 9,
                "B" to 11,
            )

        private val noteRegex = """([A-G])([#b]?)(-?\d)""".toRegex()

        /**
         * Converts a note string to a MIDI note number.
         *
         * The note string should be in the format `[A-G][#b]?-?\d`, where:
         * - `[A-G]` is the note letter.
         * - `[#b]?` is an optional accidental (`#` for sharp, `b` for flat).
         * - `[-?\d]` is the octave number.
         *
         * Some examples of valid note strings:
         * - `C4` (middle C)
         * - `D#5` (D sharp in the fifth octave)
         * - `Fb3` (F flat in the third octave)
         * - `A-1` (A in the negative first octave)
         *
         * @param note The note string.
         * @return The MIDI note number.
         * @throws IllegalArgumentException If the note string is invalid or the MIDI note number is out of range.
         */
        public operator fun get(note: String): Int {
            val match = noteRegex.matchEntire(note) ?: throw IllegalArgumentException("Invalid note: $note")
            val (letter, accidental, octave) = match.destructured
            val noteValue = notes.getValue(letter)
            val accidentalValue =
                when (accidental) {
                    "#" -> 1
                    "b" -> -1
                    else -> 0
                }
            val octaveValue = octave.toInt() * NOTES_IN_OCTAVE
            val midiNote = noteValue + accidentalValue + octaveValue
            require(midiNote in 0..MAX_MIDI_NOTE) { "Invalid MIDI note: $note ($midiNote)" }
            return midiNote
        }
    }
}
