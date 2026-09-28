/*
 * Copyright © 2026 Jacob Wysko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wysko.kmidi.midi.event

import org.wysko.kmidi.midi.NonRegisteredParameterNumber
import org.wysko.kmidi.midi.ParameterNumber
import org.wysko.kmidi.midi.RegisteredParameterNumber
import org.wysko.kmidi.midi.RpnValue
import org.wysko.kmidi.util.shl
import kotlin.experimental.or

private const val CENTS_IN_SEMITONE = 100
private const val TUNING_CENTER = 0x40
private const val FINE_TUNING_RESOLUTION = 100 / 8192.0
private const val MODULATION_DEPTH_RANGE_LSB_RESOLUTION = 100 / 128.0

/**
 * A change to a [ParameterNumber].
 *
 * *This is not an actual MIDI event, but a virtual construct for convenience.*
 *
 * @property [channel] The channel on which the change occurred.
 * @property [parameterNumber] The [ParameterNumber] that was changed.
 * @property [rpnValue] The new value of the [ParameterNumber].
 */
public sealed class VirtualParameterNumberChangeEvent(
    override val tick: Int,
    public open val channel: Byte,
    public open val parameterNumber: ParameterNumber,
    public open val rpnValue: RpnValue,
) : VirtualEvent(tick) {
    /**
     * A change to a [RegisteredParameterNumber].
     */
    public class VirtualPitchBendSensitivityChangeEvent(
        override val tick: Int,
        override val channel: Byte,
        override val rpnValue: RpnValue,
    ) : VirtualParameterNumberChangeEvent(tick, channel, RegisteredParameterNumber.PitchBendSensitivity, rpnValue) {
        /**
         * The value of the pitch bend sensitivity, in semitones.
         */
        public val value: Double = rpnValue.msb.toDouble() + rpnValue.lsb.toDouble() / CENTS_IN_SEMITONE
    }

    /**
     * A change to the fine-tuning of a channel.
     */
    public class VirtualFineTuningChangeEvent(
        override val tick: Int,
        override val channel: Byte,
        override val rpnValue: RpnValue,
    ) : VirtualParameterNumberChangeEvent(tick, channel, RegisteredParameterNumber.FineTuning, rpnValue) {
        /**
         * The value of the fine-tuning, in cents.
         */
        public val value: Double = (((rpnValue.msb shl 8) or rpnValue.lsb) - TUNING_CENTER) * FINE_TUNING_RESOLUTION
    }

    /**
     * A change to the coarse-tuning of a channel.
     */
    public class VirtualCoarseTuningChangeEvent(
        override val tick: Int,
        override val channel: Byte,
        override val rpnValue: RpnValue,
    ) : VirtualParameterNumberChangeEvent(tick, channel, RegisteredParameterNumber.CoarseTuning, rpnValue) {
        /**
         * The value of the coarse-tuning, in semitones.
         */
        public val value: Double = (rpnValue.msb - TUNING_CENTER).toDouble()
    }

    /**
     * A change to the modulation depth range.
     */
    public class VirtualModulationDepthRangeChangeEvent(
        override val tick: Int,
        override val channel: Byte,
        override val rpnValue: RpnValue,
    ) : VirtualParameterNumberChangeEvent(tick, channel, RegisteredParameterNumber.ModulationDepthRange, rpnValue) {
        /**
         * The value of the modulation depth range, in semitones.
         */
        public val value: Double = rpnValue.msb + (rpnValue.lsb * MODULATION_DEPTH_RANGE_LSB_RESOLUTION)
    }

    /**
     * A change to a [NonRegisteredParameterNumber].
     */
    public class VirtualNonRegisteredParameterNumberChangeEvent(
        override val tick: Int,
        override val channel: Byte,
        override val parameterNumber: NonRegisteredParameterNumber,
        override val rpnValue: RpnValue,
    ) : VirtualParameterNumberChangeEvent(
            tick,
            channel,
            NonRegisteredParameterNumber(rpnValue.lsb, rpnValue.msb),
            rpnValue,
        )

    public companion object {
        /**
         * Collects all [VirtualParameterNumberChangeEvent]s from a list of [MidiEvent]s.
         *
         * @receiver The list of [MidiEvent]s to collect virtual parameter number changes from.
         * @return A list of all virtual parameter number changes in the [MidiEvent]s.
         */
        public fun fromEvents(events: List<MidiEvent>): List<VirtualParameterNumberChangeEvent> {
            val list = mutableListOf<VirtualParameterNumberChangeEvent>()
            val ccEvents = events.filterIsInstance<ControlChangeEvent>()

            if (ccEvents.isEmpty()) return list

            var rpnLsb = RegisteredParameterNumber.Null.lsb
            var rpnMsb = RegisteredParameterNumber.Null.msb
            var nrpnLsb = RegisteredParameterNumber.Null.lsb
            var nrpnMsb = RegisteredParameterNumber.Null.msb
            var active = ActiveParameterFamily.None
            var dataMsb = 0x00.toByte()
            var dataLsb = 0x00.toByte()

            for (cc in ccEvents) {
                when (cc.controller) {
                    MidiConstants.Controllers.RPN_LSB -> {
                        rpnLsb = cc.value
                        active = ActiveParameterFamily.Rpn
                    }

                    MidiConstants.Controllers.RPN_MSB -> {
                        rpnMsb = cc.value
                        active = ActiveParameterFamily.Rpn
                    }

                    MidiConstants.Controllers.NRPN_LSB -> {
                        nrpnLsb = cc.value
                        active = ActiveParameterFamily.Nrpn
                    }

                    MidiConstants.Controllers.NRPN_MSB -> {
                        nrpnMsb = cc.value
                        active = ActiveParameterFamily.Nrpn
                    }

                    MidiConstants.Controllers.DATA_ENTRY_MSB, MidiConstants.Controllers.DATA_ENTRY_LSB -> {
                        if (cc.controller == MidiConstants.Controllers.DATA_ENTRY_MSB) dataMsb = cc.value else dataLsb = cc.value
                        val value = RpnValue(dataMsb, dataLsb)
                        when (active) {
                            ActiveParameterFamily.Rpn ->
                                // RPNs must be set to a non-null value
                                if (rpnLsb != RegisteredParameterNumber.Null.lsb ||
                                    rpnMsb != RegisteredParameterNumber.Null.msb
                                ) {
                                    list += createVirtualChangeEvent(ParameterNumber.from(rpnLsb, rpnMsb), cc, value)
                                }

                            ActiveParameterFamily.Nrpn ->
                                if (nrpnLsb != RegisteredParameterNumber.Null.lsb ||
                                    nrpnMsb != RegisteredParameterNumber.Null.msb
                                ) {
                                    list +=
                                        VirtualNonRegisteredParameterNumberChangeEvent(
                                            cc.tick,
                                            cc.channel,
                                            NonRegisteredParameterNumber(nrpnLsb, nrpnMsb),
                                            value,
                                        )
                                }

                            ActiveParameterFamily.None -> Unit
                        }
                    }
                }
            }
            return list
        }

        /** Which parameter number family (RPN or NRPN) was most recently selected. */
        private enum class ActiveParameterFamily { None, Rpn, Nrpn }

        private fun createVirtualChangeEvent(
            parameterNumber: ParameterNumber,
            controlChangeEvent: ControlChangeEvent,
            value: RpnValue,
        ) = when (parameterNumber) {
            is RegisteredParameterNumber.PitchBendSensitivity ->
                VirtualPitchBendSensitivityChangeEvent(controlChangeEvent.tick, controlChangeEvent.channel, value)

            is RegisteredParameterNumber.FineTuning ->
                VirtualFineTuningChangeEvent(controlChangeEvent.tick, controlChangeEvent.channel, value)

            is RegisteredParameterNumber.CoarseTuning ->
                VirtualCoarseTuningChangeEvent(controlChangeEvent.tick, controlChangeEvent.channel, value)

            is RegisteredParameterNumber.ModulationDepthRange ->
                VirtualModulationDepthRangeChangeEvent(controlChangeEvent.tick, controlChangeEvent.channel, value)

            else ->
                VirtualNonRegisteredParameterNumberChangeEvent(
                    controlChangeEvent.tick,
                    controlChangeEvent.channel,
                    NonRegisteredParameterNumber(parameterNumber.lsb, parameterNumber.msb),
                    value,
                )
        }
    }
}
