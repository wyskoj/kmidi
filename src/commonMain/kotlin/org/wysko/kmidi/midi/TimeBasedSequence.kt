package org.wysko.kmidi.midi

import org.wysko.kmidi.midi.event.Event
import org.wysko.kmidi.midi.event.MetaEvent
import org.wysko.kmidi.midi.event.NoteEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val DEFAULT_TEMPO = 500000

/**
 * An extension of a [StandardMidiFile] that provides time-based sequence information. This is useful for
 * applications that need to know the time at which each event occurs.
 */
public class TimeBasedSequence private constructor(private val standardMidiFile: StandardMidiFile) {

    /**
     * A list of [MetaEvent.SetTempo] events that occur in the sequence.
     *
     * If the [standardMidiFile] does not contain any tempo events, this list will contain
     * only the default tempo of 120 BPM at the beginning of the sequence.
     *
     * This list also removes any duplicate tempo events that occur at the same time, keeping only the last one, since
     * that one will be the active tempo.
     */
    public val tempos: List<MetaEvent.SetTempo> =
        standardMidiFile.tracks.flatMap { it.events }.filterIsInstance<MetaEvent.SetTempo>().ifEmpty {
            listOf(MetaEvent.SetTempo(0, DEFAULT_TEMPO))
        }.sortedBy { it.tick }.asReversed().distinctBy { it.tick }.asReversed().also {
            if (it.first().tick != 0) {
                it.toMutableList().add(0, MetaEvent.SetTempo(0, DEFAULT_TEMPO))
            }
        }

    private val eventTimes =
        standardMidiFile.tracks.flatMap { it.events }.associateWith { getTimeAtTick(it.tick) }.toMutableMap()

    /**
     * The total duration of the sequence.
     */
    public val duration: Duration = eventTimes.maxOf { it.value }

    /**
     * The time at which the first [NoteEvent.NoteOn] event occurs.
     *
     * If there are no [NoteEvent.NoteOn] events in the sequence, this will return `null`.
     */
    public val firstNoteOnTime: Duration? = eventTimes.filterKeys { it is NoteEvent.NoteOn }.values.minOrNull()

    /**
     * Returns the time at which the specified [event] occurs.
     *
     * If the event is not in the sequence and not registered, this will return `null`.
     *
     * @param event The event to get the time of.
     * @return The time at which the specified [event] occurs.
     * @see registerEvent
     */
    public fun getTimeOf(event: Event): Duration? = eventTimes[event]

    /**
     * Registers an event such that its time is known.
     *
     * @param event The event to register.
     * @see getTimeOf
     */
    public fun registerEvent(event: Event) {
        eventTimes[event] = getTimeAtTick(event.tick)
    }

    /**
     * Returns the duration of the sequence up to the specified [tick].
     *
     * @param tick The tick to get the time up to.
     * @return The duration of the sequence up to the specified [tick].
     */
    public fun getTimeAtTick(tick: Int): Duration {
        // If there is only one tempo event, or the tick is negative, return the time based on that tempo.
        if (tempos.size == 1 || tick < 0) return (tempos.first().secondsPerBeat * tick.toBeats()).seconds

        // Get all tempos that have started and finished before the current time.
        return tempos.filter { it.tick < tick }.foldIndexed(0.0) { index, acc, tempo ->
            val remainingTime = if (index + 1 in tempos.indices) {
                tempos[index + 1].tick.coerceAtMost(tick)
            } else {
                tick
            } - tempo.tick

            acc + remainingTime.toBeats() * tempo.secondsPerBeat
        }.seconds
    }

    /**
     * Returns the active tempo immediately before the specified [tick]. That is, any tempo event that occurs
     * at the same time as the specified [tick] will not be returned.
     *
     * @param tick The tick to get the tempo before.
     * @return The active tempo immediately before the specified [tick].
     */
    public fun getTempoBeforeTick(tick: Int): MetaEvent.SetTempo =
        if (tempos.size == 1 || tick <= 0) tempos.first() else tempos.last { it.tick < tick }

    /**
     * Returns the active tempo at the specified [tick].
     *
     * @param tick The tick to get the tempo at.
     * @return The active tempo at the specified [tick].
     */
    public fun getTempoAtTick(tick: Int): MetaEvent.SetTempo = when {
        tempos.size == 1 || tick < 0 -> tempos.first()
        else -> tempos.last { it.tick <= tick }
    }

    /**
     * Returns the active tempo at the specified [time].
     *
     * @param time The time to get the tempo at.
     * @return The active tempo at the specified [time].
     */
    public fun getTempoAtTime(time: Duration): MetaEvent.SetTempo = when {
        tempos.size == 1 || time < 0.seconds -> tempos.first()
        else -> tempos.last { eventTimes[it]!! <= time }
    }

    /**
     * Converts a list of [Arc]s to a list of [TimedArc]s.
     *
     * @param arcs The list of [Arc]s to convert.
     * @return The list of [TimedArc]s.
     */
    public fun convertArcsToTimedArcs(arcs: List<Arc>): List<TimedArc> = arcs.map { it.toTimedArc() }

    private fun Int.toBeats(): Double = this.toDouble() / standardMidiFile.header.division.ticksPerQuarterNote

    private fun Arc.toTimedArc(): TimedArc {
        val startTime = getTimeAtTick(noteOn.tick)
        val endTime = getTimeAtTick(noteOff.tick)
        return TimedArc(noteOn, noteOff, startTime, endTime)
    }

    public companion object {
        /**
         * Converts a [StandardMidiFile] to a [TimeBasedSequence].
         *
         * @receiver The [StandardMidiFile] to convert.
         */
        public fun StandardMidiFile.toTimeBasedSequence(): TimeBasedSequence = TimeBasedSequence(this)
    }
}
