package ru.agalkin.beholder

import ru.agalkin.beholder.testutils.TestAbstract
import kotlin.test.Test
import kotlin.test.assertEquals

class FlowTest : TestAbstract() {
    @Test
    fun testTeeParses() {
        assertConfigParses("tee {to stdout}", "tee {\n    to stdout;\n}\n")
    }

    @Test
    fun testJoinParses() {
        assertConfigParses("join {to stdout}", "join {\n    to stdout;\n}\n")
    }

    @Test
    fun testFlowParses() {
        assertConfigParses("flow {to stdout}", "flow {\n    to stdout;\n}\n")
    }

    @Test
    fun testFlowArguments() {
        assertConfigFails("flow whatever {to stdout}", "Too many arguments for `flow`: flow whatever [test-config:1]")
    }

    @Test
    fun testRoutingTrivial() {
        val message = Message()
        message["path"] = "start"

        val processedMessage = processMessageWithConfig(message, "set \$path '\$path, inside'")

        assertEquals("start, inside", processedMessage!!.getStringField("path"))
    }

    @Test
    fun testFlowNoneRouting() {
        val message = Message()
        message["path"] = "start"

        val processedMessage = processMessageWithConfig(message, "set \$path '\$path, before'; set \$path '\$path, after'")

        assertEquals("start, before, after", processedMessage!!.getStringField("path"))
    }

    @Test
    fun testTeeRouting() {
        val message = Message()
        message["path"] = "start"

        val processedMessage = processMessageWithConfig(message, "set \$path '\$path, before-flow'; tee {set \$path '\$path, inside-flow'} set \$path '\$path, after-flow'")

        assertEquals("start, before-flow, after-flow", processedMessage!!.getStringField("path"))
    }

    @Test
    fun testJoinRouting() {
        val message = Message()
        message["path"] = "start"

        val processedMessage = processMessageWithConfig(message, "set \$path '\$path, before-flow'; join {set \$path '\$path, inside-flow'} set \$path '\$path, after-flow'")

        assertEquals("start, before-flow, after-flow", processedMessage!!.getStringField("path"))
    }

    @Test
    fun testJoinRoutingInfiniteLoop() {
        val message = Message()
        message["path"] = "start"

        val config = "switch cat { case dog {} } " +
            "join { from udp 3820; } " +
            "tee { } "

        receiveMessagesWithConfig(config, 1) {
            sendToUdp(3820, "cat")
        }
    }

    @Test
    fun testFlowRouting() {
        val message = Message()
        message["path"] = "start"

        val processedMessage = processMessageWithConfig(message, "set \$path '\$path, before-flow'; flow {set \$path '\$path, inside-flow'} set \$path '\$path, after-flow'")

        assertEquals("start, before-flow, after-flow", processedMessage!!.getStringField("path"))
    }
}
