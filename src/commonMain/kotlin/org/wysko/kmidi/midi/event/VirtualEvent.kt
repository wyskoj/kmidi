package org.wysko.kmidi.midi.event

import org.wysko.kmidi.midi.StandardMidiFile

/**
 * A virtual event that is not an actual MIDI event, but a virtual construct for convenience.
 */
public sealed class VirtualEvent(
    override val tick: Int,
) : Event(tick) {
    public companion object {
        /**
         * Collects all [VirtualEvent]s from a [StandardMidiFile].
         *
         * @receiver The [StandardMidiFile] to collect virtual events from.
         * @return A list of all virtual events in the [StandardMidiFile].
         */
        public fun StandardMidiFile.collectVirtualEvents(): List<VirtualEvent> =
            with(tracks.flatMap { it.events }.filterIsInstance<MidiEvent>()) {
                (
                    VirtualParameterNumberChangeEvent.fromEvents(this) +
                        VirtualCompositePitchBendEvent.fromEvents(this)
                ).sortedBy { it.tick }
            }
    }
}
