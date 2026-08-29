package dev.gounthar.xcpng.gateway

import dev.gounthar.xcpng.gateway.xo.XoPowerState
import dev.gounthar.xcpng.gateway.xo.XoVm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The reachability decision, which is the one piece of judgement this connector holds.
 *
 * Every case below is a DIFFERENT observed shape from the lab pool, not four spellings of one
 * shape. That matters: a fixture set where every row agrees with the belief being tested
 * proves nothing, and the sibling project's notes record two occasions where exactly that
 * happened. The running-with-no-address rows are the ones that separate "still booting" from
 * "no agent, and no address is ever coming", which are the two the user must be told apart.
 */
class XoGatewayBridgeTest {

    private val bridge = XoGatewayBridge(client = FailingClient)

    private fun vm(
        power: XoPowerState,
        address: String? = null,
        reporting: Boolean? = null,
    ) = XoVm(
        uuid = "u",
        nameLabel = "n",
        powerState = power,
        mainIpAddress = address,
        guestIsReporting = reporting,
    )

    @Test
    fun `a running VM with an address is reachable`() {
        assertNull(bridge.unreachableBecause(vm(XoPowerState.RUNNING, address = "10.0.0.9", reporting = true)))
    }

    @Test
    fun `a halted VM is not running, whatever else it reports`() {
        // The address is deliberately present. XO really does serve one for a halted VM, so a
        // check that only ever saw halted VMs with a null address would pass while testing
        // nothing, and would miss the case that actually occurs.
        assertEquals(
            XoGatewayBridge.Unreachable.NOT_RUNNING,
            bridge.unreachableBecause(vm(XoPowerState.HALTED, address = "10.0.0.9")),
        )
    }

    @Test
    fun `suspended and paused are also not running`() {
        assertEquals(
            XoGatewayBridge.Unreachable.NOT_RUNNING,
            bridge.unreachableBecause(vm(XoPowerState.SUSPENDED)),
        )
        assertEquals(
            XoGatewayBridge.Unreachable.NOT_RUNNING,
            bridge.unreachableBecause(vm(XoPowerState.PAUSED)),
        )
    }

    @Test
    fun `running with no agent will never get an address`() {
        assertEquals(
            XoGatewayBridge.Unreachable.NO_GUEST_AGENT,
            bridge.unreachableBecause(vm(XoPowerState.RUNNING, reporting = false)),
        )
    }

    @Test
    fun `running and reporting but no address yet is worth retrying`() {
        assertEquals(
            XoGatewayBridge.Unreachable.NO_ADDRESS_YET,
            bridge.unreachableBecause(vm(XoPowerState.RUNNING, reporting = true)),
        )
    }

    @Test
    fun `running with no address and no opinion from XO is treated as still booting`() {
        // guestIsReporting is nullable because XO does not always say. Null must not fall into
        // the NO_GUEST_AGENT branch: telling a user their agent is missing when the pool never
        // made that claim is a confident wrong answer, and the retryable one is the safe read.
        assertEquals(
            XoGatewayBridge.Unreachable.NO_ADDRESS_YET,
            bridge.unreachableBecause(vm(XoPowerState.RUNNING, reporting = null)),
        )
    }

    @Test
    fun `power-on verb follows the state, and a running VM has none`() {
        assertNull(bridge.powerOnVerb(vm(XoPowerState.RUNNING, address = "10.0.0.9")))
        // Not asserting WHICH verb: that mapping is xo/'s to own and has its own test there.
        // Asserting it twice would mean two places to change and one of them would be missed.
        assertEquals(true, bridge.powerOnVerb(vm(XoPowerState.HALTED)) != null)
        assertEquals(true, bridge.powerOnVerb(vm(XoPowerState.SUSPENDED)) != null)
        assertEquals(true, bridge.powerOnVerb(vm(XoPowerState.PAUSED)) != null)
    }
}
