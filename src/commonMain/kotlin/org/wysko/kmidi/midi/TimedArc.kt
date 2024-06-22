package org.wysko.kmidi.midi

import org.wysko.kmidi.midi.event.NoteEvent
import kotlin.time.Duration

/**
 * An [Arc] that has time-based information.
 *
 * @property startTime The time at which the arc starts.
 * @property endTime The time at which the arc ends.
 */
public data class TimedArc(
    public override val noteOn: NoteEvent.NoteOn,
    public override val noteOff: NoteEvent.NoteOff,
    public val startTime: Duration,
    public val endTime: Duration
) : Arc(noteOn, noteOff) {
    /**
     * The duration of the arc.
     */
    public val duration: Duration get() = endTime - startTime
}
