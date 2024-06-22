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
 * @property [parameterNumber] The [ParameterNumber] that was changed.
 * @property [rpnValue] The new value of the [ParameterNumber].
 */
public sealed class VirtualParameterNumberChangeEvent(
    override val tick: Int,
    public open val channel: Byte,
    public open val parameterNumber: ParameterNumber,
    public open val rpnValue: RpnValue
) : VirtualEvent(tick) {

    /**
     * A change to a [RegisteredParameterNumber].
     */
    public class VirtualPitchBendSensitivityChangeEvent(
        override val tick: Int,
        override val channel: Byte,
        override val rpnValue: RpnValue
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
        override val rpnValue: RpnValue
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
        override val rpnValue: RpnValue
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
        override val rpnValue: RpnValue
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
        override val rpnValue: RpnValue
    ) : VirtualParameterNumberChangeEvent(
        tick,
        channel,
        NonRegisteredParameterNumber(rpnValue.lsb, rpnValue.msb),
        rpnValue
    )
}
