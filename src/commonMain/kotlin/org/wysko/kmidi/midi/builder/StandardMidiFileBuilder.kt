package org.wysko.kmidi.midi.builder

import org.wysko.kmidi.midi.StandardMidiFile

/**
 * A builder for [StandardMidiFile].
 */
@Suppress("MagicNumber")
public class StandardMidiFileBuilder : Builder<StandardMidiFile> {
    private val tracks = mutableListOf<StandardMidiFile.Track>()

    /**
     * The format of the MIDI file.
     *
     * @see StandardMidiFile.Header.Format
     */
    public lateinit var format: StandardMidiFile.Header.Format

    /**
     * The division of the MIDI file.
     *
     * @see StandardMidiFile.Header.Division
     */
    public lateinit var division: StandardMidiFile.Header.Division

    /**
     * The duration of a whole note in ticks.
     */
    public val Int.whole: Int
        get() = this * division.ticksPerQuarterNote * 4

    /**
     * The duration of a half note in ticks.
     */
    public val Int.half: Int
        get() = this * division.ticksPerQuarterNote * 2

    /**
     * The duration of a quarter note in ticks.
     */
    public val Int.quarter: Int
        get() = this * division.ticksPerQuarterNote

    /**
     * The duration of an eighth note in ticks.
     */
    public val Int.eighth: Int
        get() = this * division.ticksPerQuarterNote / 2

    /**
     * The duration of a sixteenth note in ticks.
     */
    public val Int.sixteenth: Int
        get() = this * division.ticksPerQuarterNote / 4

    /**
     * The duration of a thirty-second note in ticks.
     */
    public val Int.thirtySecond: Int
        get() = this * division.ticksPerQuarterNote / 8

    /**
     * The duration of a quarter note triplet in ticks.
     */
    public val Int.quarterTriplet: Int
        get() = this * division.ticksPerQuarterNote / 3 * 2

    /**
     * The duration of an eighth note triplet in ticks.
     */
    public val Int.eighthTriplet: Int
        get() = this * division.ticksPerQuarterNote / 3

    /**
     * The duration of a sixteenth note triplet in ticks.
     */
    public val Int.sixteenthTriplet: Int
        get() = this * division.ticksPerQuarterNote / 6

    /**
     * The duration of a thirty-second note triplet in ticks.
     */
    public val Int.thirtySecondTriplet: Int
        get() = this * division.ticksPerQuarterNote / 12

    /**
     * Converts a given ticks per quarter note value to a [StandardMidiFile.Header.Division.MetricalTime] instance.
     *
     * @param tpq The number of ticks per quarter note.
     * @return A [StandardMidiFile.Header.Division.MetricalTime] instance with the specified ticks per quarter note
     * value.
     * @throws IllegalArgumentException If the specified ticks per quarter note value is not in the valid range
     * [1, 32767].
     */
    public fun tpq(tpq: Int): StandardMidiFile.Header.Division.MetricalTime {
        require(tpq in 1..32767) { "Ticks per quarter note must be in the range [1, 32767]" }
        return StandardMidiFile.Header.Division.MetricalTime(tpq.toShort())
    }

    /**
     * Creates a new MIDI track using the provided block of code as a configuration builder.
     * The track is built using the [TrackBuilder].
     *
     * @param block The block of code used to configure the MIDI track.
     * @return The created [StandardMidiFile.Track].
     */
    public fun track(block: TrackBuilder.() -> Unit): StandardMidiFile.Track =
        TrackBuilder().apply(block).build().also { tracks += it }

    override fun build(): StandardMidiFile =
        StandardMidiFile(StandardMidiFile.Header("MThd", format, tracks.size.toShort(), division), tracks)
}

/**
 * Creates a new [StandardMidiFile] using the provided block of code as a configuration builder.
 *
 * @param block The block of code used to configure the [StandardMidiFile].
 * @return The created [StandardMidiFile].
 */
public fun smf(block: StandardMidiFileBuilder.() -> Unit): StandardMidiFile =
    StandardMidiFileBuilder().apply(block).build()
