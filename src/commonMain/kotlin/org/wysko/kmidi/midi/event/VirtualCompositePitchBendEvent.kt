package org.wysko.kmidi.midi.event

import org.wysko.kmidi.midi.StandardMidiFile
import org.wysko.kmidi.midi.event.VirtualParameterNumberChangeEvent.VirtualPitchBendSensitivityChangeEvent

/**
 * A change to the pitch bend of a channel, either from a [PitchWheelChangeEvent] or a
 * [VirtualPitchBendSensitivityChangeEvent].
 *
 * @property bend The new value of the pitch bend, expressed in semitones.
 */
public class VirtualCompositePitchBendEvent(
    override val tick: Int,
    public val bend: Double
) : VirtualEvent(tick) {
    public companion object {

        /**
         * Converts a list of [MidiEvent] into a list of [VirtualCompositePitchBendEvent].
         *
         * MIDI pitch bend events are dependent on the current pitch bend range, which is separate from the pitch bend
         * event itself. This method calculates the absolute semitone value for each pitch bend event based on the pitch
         * bend range.
         *
         * @param events The list of [MidiEvent] to convert.
         * @return A list of [VirtualCompositePitchBendEvent] representing the absolute semitone value of each pitch
         * bend event.
         */
        public fun fromEvents(events: List<MidiEvent>): List<VirtualCompositePitchBendEvent> {
            var pitchWheel = 0.0
            var pitchBendRange = 2.0

            val sensitivityEvents = VirtualParameterNumberChangeEvent.fromEvents(events)
                .filterIsInstance<VirtualPitchBendSensitivityChangeEvent>()
            val bendEvents = events.filterIsInstance<PitchWheelChangeEvent>()

            val inputEvents = (sensitivityEvents + bendEvents).sortedBy { it.tick }
            val outputEvents = mutableListOf<VirtualCompositePitchBendEvent>()

            for (event in inputEvents) {
                when (event) {
                    is VirtualPitchBendSensitivityChangeEvent -> {
                        pitchBendRange = event.value
                    }

                    is PitchWheelChangeEvent -> {
                        pitchWheel = event.semitones
                    }

                    else -> {
                        // Do nothing.
                    }
                }

                outputEvents += VirtualCompositePitchBendEvent(
                    event.tick, pitchWheel * pitchBendRange
                )
            }

            return outputEvents
        }
    }
}
