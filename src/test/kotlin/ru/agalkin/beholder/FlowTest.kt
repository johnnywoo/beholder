package ru.agalkin.beholder

import org.junit.Test
import kotlin.test.assertEquals

class FlowTest : TestAbstract() {
    @Test
    fun testFlowParses() {
        assertConfigParses("flow {to stdout}", "flow {\n    to stdout;\n}\n")
    }

    @Test
    fun testFlowOutParses() {
        assertConfigParses("flow out {to stdout}", "flow out {\n    to stdout;\n}\n")
    }

    @Test
    fun testFlowClosedParses() {
        assertConfigParses("flow closed {to stdout}", "flow closed {\n    to stdout;\n}\n")
    }

    @Test
    fun testFlowBadMode() {
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
    fun testFlowRoutingDefault() {
        val message = Message()
        message["path"] = "start"

        val processedMessage = processMessageWithConfig(message, "set \$path '\$path, before-flow'; flow {set \$path '\$path, inside-flow'} set \$path '\$path, after-flow'")

        assertEquals("start, before-flow, after-flow", processedMessage!!.getStringField("path"))
    }

    @Test
    fun testFlowRoutingOut() {
        val message = Message()
        message["path"] = "start"

        val processedMessage = processMessageWithConfig(message, "set \$path '\$path, before-flow'; flow out {set \$path '\$path, inside-flow'} set \$path '\$path, after-flow'")

        assertEquals("start, before-flow, after-flow", processedMessage!!.getStringField("path"))
    }

    @Test
    fun testFlowRoutingClosed() {
        val message = Message()
        message["path"] = "start"

        val processedMessage = processMessageWithConfig(message, "set \$path '\$path, before-flow'; flow closed {set \$path '\$path, inside-flow'} set \$path '\$path, after-flow'")

        assertEquals("start, before-flow, after-flow", processedMessage!!.getStringField("path"))
    }
}
