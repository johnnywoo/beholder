package ru.agalkin.beholder.passthrough

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.testutils.TestAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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

        val processedMessage = processMessageWithConfig(message, """
            switch 'cat' {
                case ~cat~ { set ¥animal 'feline' }
            }
        """.replace('¥', '$'))

        assertNotNull(processedMessage)
        assertEquals("feline", processedMessage.getStringField("animal"))
    }

    @Test
    fun testSwitchMultipleCasesFirstMatch() {
        val message = Message()

        val processedMessage = processMessageWithConfig(message, """
            switch 'cat' {
                case ~cat~ { set ¥animal 'feline' }
                case ~dog~ { set ¥animal 'canine' }
            }
        """.replace('¥', '$'))

        assertNotNull(processedMessage)
        assertEquals("feline", processedMessage.getStringField("animal"))
    }

    @Test
    fun testSwitchMultipleCasesMiddleMatch() {
        val message = Message()

        val processedMessage = processMessageWithConfig(message, """
            switch 'dog' {
                case ~cat~ { set ¥animal 'feline' }
                case ~dog~ { set ¥animal 'canine' }
                case ~tiger~ { set ¥animal 'feline' }
            }
        """.replace('¥', '$'))

        assertNotNull(processedMessage)
        assertEquals("canine", processedMessage.getStringField("animal"))
    }

    @Test
    fun testSwitchMultipleCasesLastMatch() {
        val message = Message()

        val processedMessage = processMessageWithConfig(message, """
            switch 'dog' {
                case ~cat~ { set ¥animal 'feline' }
                case ~dog~ { set ¥animal 'canine' }
            }
        """.replace('¥', '$'))

        assertNotNull(processedMessage)
        assertEquals("canine", processedMessage.getStringField("animal"))
    }

    @Test
    fun testSwitchDefault() {
        val message = Message()

        val processedMessage = processMessageWithConfig(message, """
            switch 'dog' {
                case ~cat~ { set ¥animal 'feline' }
                default { set ¥animal 'canine' }
            }
        """.replace('¥', '$'))

        assertNotNull(processedMessage)
        assertEquals("canine", processedMessage.getStringField("animal"))
    }

    @Test
    fun testSwitchEmptyBlock() {
        val message = Message.of("animal" to "initial")

        val processedMessage = processMessageWithConfig(message, """
            switch 'cat' {
                case ~cat~ {}
                default { set ¥animal 'unknown' }
            }
        """.replace('¥', '$'))

        assertNotNull(processedMessage)
        assertEquals("initial", processedMessage.getStringField("animal"))
    }

    @Test
    fun testSwitchEmptyDefault() {
        val message = Message.of("animal" to "initial")

        val processedMessage = processMessageWithConfig(message, """
            switch 'dog' {
                case ~cat~ {}
                default { set ¥animal 'unknown' }
            }
        """.replace('¥', '$'))

        assertNotNull(processedMessage)
        assertEquals("unknown", processedMessage.getStringField("animal"))
    }

    @Test
    fun testSwitchOnlyDefault() {
        val message = Message.of("animal" to "initial")

        val processedMessage = processMessageWithConfig(message, """
            switch 'dog' {
                default { set ¥animal 'unknown' }
            }
        """.replace('¥', '$'))

        assertNotNull(processedMessage)
        assertEquals("unknown", processedMessage.getStringField("animal"))
    }

    @Test
    fun testSwitchFlowCaseRouting() {
        val message = Message.of("animal" to "initial")

        val processedMessage = processMessageWithConfig(message, """
            switch 'dog' {
                case ~dog~ {
                    set ¥animal 'canine';
                    tee {set ¥animal 'error'}
                }
            }
        """.replace('¥', '$'))

        assertNotNull(processedMessage)
        assertEquals("canine", processedMessage.getStringField("animal"))
    }

    @Test
    fun testSwitchFlowDefaultRouting() {
        val message = Message.of("animal" to "initial")

        val processedMessage = processMessageWithConfig(message, """
            switch 'dog' {
                default {
                    set ¥animal 'unknown';
                    tee {set ¥animal 'error'}
                }
            }
        """.replace('¥', '$'))

        assertNotNull(processedMessage)
        assertEquals("unknown", processedMessage.getStringField("animal"))
    }

    @Test
    fun testSwitchMatchLiteral() {
        val message = Message.of("animal" to "initial")

        val processedMessage = processMessageWithConfig(message, """
            switch 'dog' {
                case dog { set ¥animal 'ok' }
            }
        """.replace('¥', '$'))

        assertNotNull(processedMessage)
        assertEquals("ok", processedMessage.getStringField("animal"))
    }

    @Test
    fun testSwitchMatchQuoted() {
        val message = Message.of("animal" to "initial")

        val processedMessage = processMessageWithConfig(message, """
            switch 'dog' {
                case 'dog' { set ¥animal 'ok' }
            }
        """.replace('¥', '$'))

        assertNotNull(processedMessage)
        assertEquals("ok", processedMessage.getStringField("animal"))
    }

    @Test
    fun testSwitchMatchTemplate() {
        val message = Message.of(
            "feline" to "cat",
            "animal" to "cat"
        )

        val processedMessage = processMessageWithConfig(message, """
            switch ¥animal {
                case ¥feline { set ¥animal 'ok' }
            }
        """.replace('¥', '$'))

        assertNotNull(processedMessage)
        assertEquals("ok", processedMessage.getStringField("animal"))
    }

    @Test
    fun testSwitchNoDefaultFallthrough() {
        val message = Message.of("animal" to "initial")

        val processedMessage = processMessageWithConfig(message, """
            switch cat {
                case dog { set ¥animal 'error' }
            }
        """.replace('¥', '$'))

        assertNotNull(processedMessage)
        assertEquals("initial", processedMessage.getStringField("animal"))
    }

    @Test
    fun testSwitchNoDefaultOnCaseMatch() {
        val message = Message.of(
            "animal" to "initial",
            "is_default_visited" to "no"
        )

        val processedMessage = processMessageWithConfig(message, """
            switch cat {
                case cat { set ¥animal 'feline' }
                default { set ¥is_default_visited 'yes' }
            }
        """.replace('¥', '$'))

        assertNotNull(processedMessage)
        assertEquals("no", processedMessage.getStringField("is_default_visited"))
    }

    @Test
    fun testSwitchNoDefaultOnCaseMatchRegexp() {
        val message = Message.of(
            "animal" to "initial",
            "is_default_visited" to "no"
        )

        val processedMessage = processMessageWithConfig(message, """
            switch cat {
                case ~cat~ { set ¥animal 'feline' }
                default { set ¥is_default_visited 'yes' }
            }
        """.replace('¥', '$'))

        assertNotNull(processedMessage)
        assertEquals("no", processedMessage.getStringField("is_default_visited"))
    }

    @Test
    fun testSwitchAndCommand() {
        val message = Message()

        val processedMessage = processMessageWithConfig(message, """
            set ¥before true;
            switch cat { case cat {set ¥case true;} }
            set ¥after true;
        """.replace('¥', '$'))

        assertNotNull(processedMessage)
        assertEquals("true", processedMessage.getStringField("after"))
    }

    @Test
    fun testSwitchAndCommandDefault() {
        val message = Message()

        val processedMessage = processMessageWithConfig(message, """
            set ¥before true;
            switch cat {
                case cat {set ¥case true;}
                default {set ¥default true;}
            }
            set ¥after true;
        """.replace('¥', '$'))

        assertNotNull(processedMessage)
        assertEquals("true", processedMessage.getStringField("after"))
    }

    @Test
    fun testSwitchAndCommandDrop() {
        val message = Message()

        val processedMessage = processMessageWithConfig(message, """
            set ¥before true;
            switch cat {
                case cat {set ¥case true;}
                case dog {drop}
            }
            set ¥after true;
        """.replace('¥', '$'))

        assertNotNull(processedMessage)
        assertEquals("true", processedMessage.getStringField("after"))
    }

    @Test
    fun testSwitchAndCommandDefaultDrop() {
        val message = Message()

        val processedMessage = processMessageWithConfig(message, """
            set ¥before true;
            switch cat {
                case cat {set ¥case true}
                default {drop}
            }
            set ¥after true;
        """.replace('¥', '$'))

        assertNotNull(processedMessage)
        assertEquals("true", processedMessage.getStringField("after"))
    }

    @Test
    fun testSwitchMultiplicator() {
        val message = Message.of(
            "cat" to "feline",
            "dog" to "canine"
        )

        val processedMessage = processMessageWithConfig(message, """
            parse each-field-as-message;
            switch ¥key {
                case cat {}
                default {drop}
            }
        """.replace('¥', '$'))

        assertNotNull(processedMessage)
        assertEquals("cat", processedMessage.getStringField("key"))
    }
}
