package ru.agalkin.beholder

import org.junit.Test
import kotlin.test.assertEquals

class SwitchTest : TestAbstract() {
    @Test
    fun testSwitchParses() {
        assertConfigParses("switch 'cat' {case ~.~}", "switch 'cat' {\n    case ~.~;\n}\n")
    }

    @Test
    fun testSwitchNoArgument() {
        assertConfigFails("switch {default}", "`switch` needs an argument: switch [test-config:1]")
    }

    @Test
    fun testSwitchTwoDefaults() {
        assertConfigFails("switch 'cat' {default; default}", "`switch` cannot have multiple `default` subcommands: default [test-config:1]")
    }

    @Test
    fun testSwitchWorks() {
        val message = Message()
        message["payload"] = "We've got cats and dogs"

        val processedMessage = processMessageWithConfig(message, "switch 'cat' { case ~cat~ { set \$animal 'feline' } default { set \$animal 'alien' }}")

        assertEquals("feline", processedMessage!!.getStringField("animal"))
    }
}
