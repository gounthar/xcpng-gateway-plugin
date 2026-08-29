package dev.gounthar.xcpng.gateway

import dev.gounthar.xcpng.gateway.xo.XoClient
import dev.gounthar.xcpng.gateway.xo.XoPowerState
import dev.gounthar.xcpng.gateway.xo.XoVm
import dev.gounthar.xcpng.gateway.xo.resumeVerb

/**
 * The seam between Xen Orchestra and whatever platform is hosting this plugin.
 *
 * This file exists to hold the split that made the toolbox sibling worth reusing at all: its
 * `xo/` package is 1121 lines with zero JetBrains imports, so it ports to any JVM host, while
 * its 1745-line shell is welded to one. Everything platform-facing in this repository goes
 * above this file, everything platform-independent below it.
 *
 * The rule to hold: if a class under `xo/` ever needs a `com.intellij` or `com.jetbrains`
 * import, the design has gone wrong and the fix is here, not there.
 *
 * ## What a connector owns, and what it does not
 *
 * A connector, Gateway or Toolbox, hands the IDE a machine and an SSH endpoint, and stops.
 * Once the backend is running on the VM the IDE treats it as any other remote backend. So the
 * surface here is small on purpose: enumerate, decide whether a VM is reachable, power one
 * on. Remote toolchain detection and remote debugging are backend-side and cannot be added
 * from a connector by anyone, whatever hypervisor is underneath.
 *
 * Anything that has to appear INSIDE the running IDE (a tool window, an action, a settings
 * panel) belongs in the sibling xcpng-intellij-plugin, not here. That boundary is the reason
 * these are two repositories instead of one.
 *
 * ## Read before adding an address check
 *
 * There is a real trap in XO's addressing, and the vendored client ALREADY HANDLES IT. XO
 * serves a VM's last known address whether or not it is running, and on the lab pool 4 of 6
 * halted VMs reported one against 1 of 4 running ones. [dev.gounthar.xcpng.gateway.xo.XoRestClient]
 * drops `mainIpAddress` unless `power_state` is Running, so [XoVm.mainIpAddress] is already
 * trustworthy by the time it reaches this file.
 *
 * That is worth stating because the obvious next move is to add a staleness guard here, and
 * it would be a second guard over a solved problem: dead code that looks like diligence.
 *
 * The part that is NOT solved is the other half. A guest with no management agent reports no
 * address ever, and [XoVm.guestIsReporting] is how to tell "not booted yet" from "will never
 * have one". Those two need different messages to the user, and choosing them is UI work,
 * which is why it is not done here.
 */
class XoGatewayBridge(private val client: XoClient) {

    /**
     * Why a VM cannot be connected to, or null when it can.
     *
     * Modelled as a reason rather than a boolean because every value here becomes a different
     * sentence on screen, and collapsing them to `false` is what produces the generic
     * "could not connect" that teaches a user nothing.
     */
    enum class Unreachable {
        /** Powered off, paused or suspended. [powerOnVerb] says how to fix it. */
        NOT_RUNNING,

        /** Running, and the guest is not reporting to the host. No agent, so no address ever. */
        NO_GUEST_AGENT,

        /** Running and reporting, but no address yet. Usually still booting. Worth retrying. */
        NO_ADDRESS_YET,
    }

    /**
     * Whether the IDE could be pointed at this VM right now, and if not, why.
     *
     * Pure, and takes the VM rather than reading one, so it is testable without a pool.
     */
    fun unreachableBecause(vm: XoVm): Unreachable? = when {
        vm.powerState != XoPowerState.RUNNING -> Unreachable.NOT_RUNNING
        vm.mainIpAddress != null -> null
        vm.guestIsReporting == false -> Unreachable.NO_GUEST_AGENT
        else -> Unreachable.NO_ADDRESS_YET
    }

    /**
     * The XO verb that powers this VM on from its current state, or null when it is already
     * running or XO did not say.
     *
     * A thin pass-through to [resumeVerb] on purpose. The mapping is subtle (a `start` on a
     * suspended VM fails in a way the user reads as a broken plugin) and it must have exactly
     * one implementation, which is the one in `xo/`.
     */
    fun powerOnVerb(vm: XoVm): (suspend (XoClient, XoVm) -> Unit)? = vm.powerState.resumeVerb

    /** Every VM on the pool, as XO reports it. */
    suspend fun machines(): List<XoVm> = client.listVms()

    /**
     * One VM, re-read from the pool.
     *
     * Always re-read after acting, never assume. The sibling records that the events stream
     * serves the raw XO object rather than the REST view, so the two disagree on shape and
     * only one of them is what the rest of this code expects.
     */
    suspend fun refresh(vm: XoVm): XoVm? = client.getVm(vm.uuid)
}
