package org.wysko.kmidi.midi

import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.kmidi.midi.event.NoteEvent.NoteOff
import org.wysko.kmidi.midi.event.NoteEvent.NoteOn

private const val MIDI_NOTES_RANGE = 128

/**
 * An Arc is the pair of a [NoteOn] and a [NoteOff] event.
 *
 * @property noteOn The [NoteOn] event.
 * @property noteOff The [NoteOff] event.
 */
public open class Arc(
    internal open val noteOn: NoteOn,
    internal open val noteOff: NoteOff
) {
    /**
     * The note value of the arc.
     */
    public val note: Byte get() = noteOn.note

    /**
     * The channel of the arc.
     */
    public val channel: Byte get() = noteOn.channel

    /**
     * The velocity of the arc.
     */
    public val velocity: Byte get() = noteOn.velocity

    public companion object {
        /**
         * Converts a list of [NoteEvent] objects into a list of [Arc] objects.
         *
         * @param noteEvents The list of [NoteEvent] objects to convert.
         * @return The list of [Arc] objects generated from the [noteEvents].
         */
        public fun fromNoteEvents(noteEvents: List<NoteEvent>): List<Arc> {
            val arcs = mutableListOf<Arc>()
            val onEvents = arrayOfNulls<NoteOn>(MIDI_NOTES_RANGE)

            noteEvents.forEach { noteEvent ->
                val noteInt = noteEvent.note.toInt()

                when (noteEvent) {
                    is NoteOn ->
                        // If the same note starts again while it is playing, ignore this new NoteOn event
                        if (onEvents[noteInt] == null) {
                            onEvents[noteInt] = noteEvent
                        }

                    is NoteOff -> onEvents[noteInt]?.let {
                        arcs.add(
                            Arc(it, noteEvent),
                        )
                        onEvents[noteInt] = null
                    }
                }
            }

            // Remove exact duplicates
            return arcs.distinct()
        }
    }
}
