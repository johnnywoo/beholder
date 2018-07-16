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
    fun testSwitchFlow() {
        assertConfigParses(
            """
                switch x {
                    case ~x~ {
                        tee {drop}
                    }
                }

            """,
            """
            |switch x {
            |    case ~x~ {
            |        tee {
            |            drop;
            |        }
            |    }
            |}
            |""".trimMargin()
        )
    }

    @Test
    fun testSwitchWorks() {
        val message = Message()

        val processedMessage = processMessageWithConfig(message, "switch 'cat' { case ~cat~ { set \$animal 'feline' } }")

        assertEquals("feline", processedMessage!!.getStringField("animal"))
    }

    @Test
    fun testSwitchMultipleCasesFirstMatch() {
        val message = Message()

        val processedMessage = processMessageWithConfig(message, "switch 'cat' { case ~cat~ { set \$animal 'feline' } case ~dog~ { set \$animal 'canine' } }")

        assertEquals("feline", processedMessage!!.getStringField("animal"))
    }

    @Test
    fun testSwitchMultipleCasesMiddleMatch() {
        val message = Message()

        val processedMessage = processMessageWithConfig(message, "switch 'dog' { case ~cat~ { set \$animal 'feline' } case ~dog~ { set \$animal 'canine' } case ~tiger~ { set \$animal 'feline' } }")

        assertEquals("canine", processedMessage!!.getStringField("animal"))
    }

    @Test
    fun testSwitchMultipleCasesLastMatch() {
        val message = Message()

        val processedMessage = processMessageWithConfig(message, "switch 'dog' { case ~cat~ { set \$animal 'feline' } case ~dog~ { set \$animal 'canine' } }")

        assertEquals("canine", processedMessage!!.getStringField("animal"))
    }

    @Test
    fun testSwitchDefault() {
        val message = Message()

        val processedMessage = processMessageWithConfig(message, "switch 'dog' { case ~cat~ { set \$animal 'feline' } default { set \$animal 'canine' } }")

        assertEquals("canine", processedMessage!!.getStringField("animal"))
    }

    @Test
    fun testSwitchEmptyBlock() {
        val message = Message()
        message["animal"] = "initial"

        val processedMessage = processMessageWithConfig(message, "switch 'cat' { case ~cat~ {} default { set \$animal 'unknown' } }")

        assertEquals("initial", processedMessage!!.getStringField("animal"))
    }

    @Test
    fun testSwitchEmptyDefault() {
        val message = Message()
        message["animal"] = "initial"

        val processedMessage = processMessageWithConfig(message, "switch 'dog' { case ~cat~ {} default { set \$animal 'unknown' } }")

        assertEquals("unknown", processedMessage!!.getStringField("animal"))
    }

    @Test
    fun testSwitchOnlyDefault() {
        val message = Message()
        message["animal"] = "initial"

        val processedMessage = processMessageWithConfig(message, "switch 'dog' { default { set \$animal 'unknown' } }")

        assertEquals("unknown", processedMessage!!.getStringField("animal"))
    }

    @Test
    fun testSwitchFlowCaseRouting() {
        val message = Message()
        message["animal"] = "initial"

        val processedMessage = processMessageWithConfig(message, "switch 'dog' { case ~dog~ { set \$animal 'canine'; tee {set \$animal 'error'} } }")

        assertEquals("canine", processedMessage!!.getStringField("animal"))
    }

    @Test
    fun testSwitchFlowDefaultRouting() {
        val message = Message()
        message["animal"] = "initial"

        val processedMessage = processMessageWithConfig(message, "switch 'dog' { default { set \$animal 'unknown'; tee {set \$animal 'error'} } }")

        assertEquals("unknown", processedMessage!!.getStringField("animal"))
    }

    @Test
    fun testSwitchMatchLiteral() {
        val message = Message()
        message["animal"] = "initial"

        val processedMessage = processMessageWithConfig(message, "switch 'dog' { case dog { set \$animal 'ok' } }")

        assertEquals("ok", processedMessage!!.getStringField("animal"))
    }

    @Test
    fun testSwitchMatchQuoted() {
        val message = Message()
        message["animal"] = "initial"

        val processedMessage = processMessageWithConfig(message, "switch 'dog' { case 'dog' { set \$animal 'ok' } }")

        assertEquals("ok", processedMessage!!.getStringField("animal"))
    }

    @Test
    fun testSwitchMatchTemplate() {
        val message = Message()
        message["feline"] = "cat"
        message["animal"] = "cat"

        val processedMessage = processMessageWithConfig(message, "switch \$animal { case \$feline { set \$animal 'ok' } }")

        assertEquals("ok", processedMessage!!.getStringField("animal"))
    }
}
