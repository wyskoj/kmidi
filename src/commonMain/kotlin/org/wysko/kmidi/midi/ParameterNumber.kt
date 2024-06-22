package org.wysko.kmidi.midi

/**
 * A pair of bytes that specify a parameter in a MIDI device.
 *
 * @property lsb The least significant byte of the PN.
 * @property msb The most significant byte of the PN.
 */
public sealed class ParameterNumber protected constructor(public open val lsb: Byte, public open val msb: Byte) {
    public companion object {
        /**
         * Creates a [ParameterNumber] from the given least and most significant bytes.
         *
         * @param lsb The least significant byte of the PN.
         * @param msb The most significant byte of the PN.
         * @return A [ParameterNumber] that corresponds to the given bytes.
         */
        public fun from(lsb: Byte, msb: Byte): ParameterNumber = when (msb) {
            0x0.toByte() -> when (lsb) {
                0x0.toByte() -> RegisteredParameterNumber.PitchBendSensitivity
                0x1.toByte() -> RegisteredParameterNumber.FineTuning
                0x2.toByte() -> RegisteredParameterNumber.CoarseTuning
                0x5.toByte() -> RegisteredParameterNumber.ModulationDepthRange
                else -> NonRegisteredParameterNumber(lsb, msb)
            }

            0x7F.toByte() -> RegisteredParameterNumber.Null
            else -> NonRegisteredParameterNumber(lsb, msb)
        }
    }
}

/**
 * A [ParameterNumber] that is registered with the MIDI Manufacturer's Association.
 *
 * @property lsb The least significant byte of the RPN.
 * @property msb The most significant byte of the RPN.
 */
@Suppress("MagicNumber")
public sealed class RegisteredParameterNumber(public override val lsb: Byte, public override val msb: Byte) :
    ParameterNumber(lsb, msb) {

    /**
     * Sets the sensitivity of Pitch Bend.
     *
     * The MSB of Data Entry represents the sensitivity in semitones,
     * and the LSB of Data Entry represents the sensitivity in cents.
     *
     * For example, a value of `MSB = 01`, `LSB = 00` means +/- 1 semitone (a total range of two semitones).
     *
     * The GM2 device shall be able to accommodate at least +/-12 semitones.
     */
    public data object PitchBendSensitivity : RegisteredParameterNumber(0x0, 0x0)

    /**
     * Sets fine-tuning of the channel.
     */
    public data object FineTuning : RegisteredParameterNumber(0x0, 0x1)

    /**
     * Sets coarse-tuning of the channel.
     */
    public data object CoarseTuning : RegisteredParameterNumber(0x0, 0x2)

    /**
     *
     * Sets the peak value of Vibrato or LFO Pitch change amount
     * from the basic pitch set by the Modulation Depth controller (cc#1).
     *
     * The value `1` of MSB of the Data Entry corresponds to a semitone,
     * and `1` of LSB corresponds to 100/128 Cents.
     *
     * For example, `MSB = 01H`, `LSB = 00H` means that the Mod Wheel
     * will modulate a maximum of +/- one semitone of vibrato depth
     * (that is, two semitones peak to peak, or one semitone from the center frequency in either direction).
     *
     * Another example, `MSB = 00H`, `LSB = 08H` means that the vibrato depth
     * will be 6.25 cents in either direction from the center frequency.
     */
    public data object ModulationDepthRange : RegisteredParameterNumber(0x0, 0x5)

    /**
     * Data entry events are ignored.
     */
    public data object Null : RegisteredParameterNumber(0x7F, 0x7F)
}

/**
 * A [ParameterNumber] that is not registered with the MIDI Manufacturer's Association.
 *
 * @property lsb The least significant byte of the NRPN.
 * @property msb The most significant byte of the NRPN.
 */
public data class NonRegisteredParameterNumber(override val lsb: Byte, override val msb: Byte) :
    ParameterNumber(lsb, msb)

/**
 * A value for a [ParameterNumber].
 *
 * @property msb The most significant byte of the value.
 * @property lsb The least significant byte of the value.
 */
public data class RpnValue(public val msb: Byte, public val lsb: Byte)
