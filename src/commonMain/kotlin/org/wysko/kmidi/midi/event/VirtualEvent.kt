package org.wysko.kmidi.midi.event

import org.wysko.kmidi.midi.NonRegisteredParameterNumber
import org.wysko.kmidi.midi.ParameterNumber
import org.wysko.kmidi.midi.RegisteredParameterNumber.*
import org.wysko.kmidi.midi.RpnValue
import org.wysko.kmidi.midi.StandardMidiFile
import org.wysko.kmidi.midi.event.MidiConstants.Controllers
import org.wysko.kmidi.midi.event.VirtualParameterNumberChangeEvent.*

/**
 * A virtual event that is not an actual MIDI event, but a virtual construct for convenience.
 */
public sealed class VirtualEvent(override val tick: Int) : Event(tick) {
    public companion object {
        /**
         * Collects all [VirtualEvent]s from a [StandardMidiFile].
         *
         * @receiver The [StandardMidiFile] to collect virtual events from.
         * @return A list of all virtual events in the [StandardMidiFile].
         */
        public fun StandardMidiFile.collectVirtualEvents(): List<VirtualEvent> {
            val parameterNumberChanges = collectParameterNumberChanges()
            return listOf(parameterNumberChanges).flatten().sortedBy { it.tick }
        }

        private fun StandardMidiFile.collectParameterNumberChanges(): List<VirtualParameterNumberChangeEvent> {
            val list = mutableListOf<VirtualParameterNumberChangeEvent>()
            val events = tracks.flatMap { it.events }.filterIsInstance<ControlChangeEvent>()

            if (events.isEmpty()) return list

            val controllers = mutableMapOf(
                Controllers.RPN_LSB to Null.lsb,
                Controllers.RPN_MSB to Null.msb,
                Controllers.DATA_ENTRY_LSB to 0x00.toByte(),
                Controllers.DATA_ENTRY_MSB to 0x00.toByte()
            )

            return events.fold(list) { acc: MutableList<VirtualParameterNumberChangeEvent>,
                                       controlChangeEvent: ControlChangeEvent ->

                controllers[controlChangeEvent.controller] = controlChangeEvent.value

                // RPNs must be set to a non-null value
                if (controllers[Controllers.RPN_LSB] == Null.lsb && controllers[Controllers.RPN_MSB] == Null.msb) {
                    return@fold acc
                }

                // RPNs are set. Are we entering data?
                if (controlChangeEvent.controller == Controllers.DATA_ENTRY_LSB ||
                    controlChangeEvent.controller == Controllers.DATA_ENTRY_MSB
                ) {
                    val parameterNumber = ParameterNumber.from(
                        controllers[Controllers.RPN_LSB]!!,
                        controllers[Controllers.RPN_MSB]!!
                    )
                    acc += createVirtualChangeEvent(parameterNumber, controlChangeEvent, controllers)
                }
                acc
            }
        }

        private fun createVirtualChangeEvent(
            parameterNumber: ParameterNumber,
            controlChangeEvent: ControlChangeEvent,
            controllers: MutableMap<Byte, Byte>
        ) = when (parameterNumber) {
            is PitchBendSensitivity -> VirtualPitchBendSensitivityChangeEvent(
                controlChangeEvent.tick,
                controlChangeEvent.channel,
                RpnValue(
                    controllers[Controllers.DATA_ENTRY_LSB]!!,
                    controllers[Controllers.DATA_ENTRY_MSB]!!
                )
            )

            is FineTuning -> VirtualFineTuningChangeEvent(
                controlChangeEvent.tick,
                controlChangeEvent.channel,
                RpnValue(
                    controllers[Controllers.DATA_ENTRY_LSB]!!,
                    controllers[Controllers.DATA_ENTRY_MSB]!!
                )
            )

            is CoarseTuning -> VirtualCoarseTuningChangeEvent(
                controlChangeEvent.tick,
                controlChangeEvent.channel,
                RpnValue(
                    controllers[Controllers.DATA_ENTRY_LSB]!!,
                    controllers[Controllers.DATA_ENTRY_MSB]!!
                )
            )

            is ModulationDepthRange -> VirtualModulationDepthRangeChangeEvent(
                controlChangeEvent.tick,
                controlChangeEvent.channel,
                RpnValue(
                    controllers[Controllers.DATA_ENTRY_LSB]!!,
                    controllers[Controllers.DATA_ENTRY_MSB]!!
                )
            )

            else -> VirtualNonRegisteredParameterNumberChangeEvent(
                controlChangeEvent.tick,
                controlChangeEvent.channel,
                parameterNumber as NonRegisteredParameterNumber,
                RpnValue(
                    controllers[Controllers.DATA_ENTRY_LSB]!!,
                    controllers[Controllers.DATA_ENTRY_MSB]!!
                )
            )
        }
    }
}
