package ru.agalkin.beholder

import org.junit.Test

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
        assertConfigFails("flow whatever {to stdout}", "Too many arguments for `flow`: flow whatever")
    }
}
