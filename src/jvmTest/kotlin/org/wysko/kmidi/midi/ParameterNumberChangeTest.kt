package org.wysko.kmidi.midi

import org.junit.Test
import org.wysko.kmidi.midi.event.ControlChangeEvent
import org.wysko.kmidi.midi.event.VirtualParameterNumberChangeEvent
import org.wysko.kmidi.midi.event.VirtualParameterNumberChangeEvent.VirtualNonRegisteredParameterNumberChangeEvent
import org.wysko.kmidi.midi.event.VirtualParameterNumberChangeEvent.VirtualPitchBendSensitivityChangeEvent
import kotlin.test.assertEquals

class ParameterNumberChangeTest {
    private fun cc(
        controller: Int,
        value: Int,
    ) = ControlChangeEvent(0, 0, controller.toByte(), value.toByte())

    @Test
    fun `NRPN data entry is not attributed to previously selected RPN`() {
        val events = mutableListOf(cc(101, 0), cc(100, 0), cc(6, 12))
        val nrpns = listOf(8 to 64, 9 to 64, 10 to 64, 32 to 89, 33 to 69, 99 to 54, 100 to 74, 102 to 64)
        nrpns.forEach { (lsb, data) ->
            events += listOf(cc(99, 1), cc(98, lsb), cc(6, data))
        }

        val result = VirtualParameterNumberChangeEvent.fromEvents(events)

        val bends = result.filterIsInstance<VirtualPitchBendSensitivityChangeEvent>()
        assertEquals(listOf(12.0), bends.map { it.value })

        val nrpnEvents = result.filterIsInstance<VirtualNonRegisteredParameterNumberChangeEvent>()
        assertEquals(nrpns.size, nrpnEvents.size)
        assertEquals(nrpns.map { it.first.toByte() }, nrpnEvents.map { it.parameterNumber.lsb })
        assertEquals(nrpns.map { it.second.toByte() }, nrpnEvents.map { it.rpnValue.msb })
    }
}
