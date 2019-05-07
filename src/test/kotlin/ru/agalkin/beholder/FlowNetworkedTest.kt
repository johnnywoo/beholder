package ru.agalkin.beholder

import ru.agalkin.beholder.testutils.NetworkedTestAbstract
import kotlin.test.Test

class FlowNetworkedTest : NetworkedTestAbstract() {
    @Test
    fun testJoinRoutingInfiniteLoop() {
        val config = "switch cat { case dog {} } " +
            "join { from udp 3820; } " +
            "tee { } "

        feedMessagesIntoConfig(config, 1) {
            sendToUdp(3820, "cat")
        }
    }
}
