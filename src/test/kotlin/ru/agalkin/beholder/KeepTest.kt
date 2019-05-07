package ru.agalkin.beholder

import ru.agalkin.beholder.testutils.TestAbstract
import ru.agalkin.beholder.testutils.assertFieldNames
import kotlin.test.Test

class KeepTest : TestAbstract() {
    @Test
    fun testKeepParses() {
        assertConfigParses("keep \$a", "keep \$a;\n")
    }

    @Test
    fun testKeepParsesMultiple() {
        assertConfigParses("keep \$a \$b", "keep \$a \$b;\n")
    }

    @Test
    fun testKeepFailsNoArgs() {
        assertConfigFails("keep", "`keep` needs at least one field name: keep [test-config:1]")
    }

    @Test
    fun testKeepFailsStringArg() {
        assertConfigFails("keep \$a 'b'", "All arguments of `keep` must be field names: keep \$a 'b' [test-config:1]")
    }

    @Test
    fun testKeepFailsRegexpArg() {
        assertConfigFails("keep \$a ~b~", "All arguments of `keep` must be field names: keep \$a ~b~ [test-config:1]")
    }

    @Test
    fun testKeepWorks() {
        val message = Message.of(
            "removed" to "Removed",
            "payload" to "We've got cats and dogs"
        )

        val processedMessage = processMessageWithConfig(message, "keep \$payload")

        assertFieldNames(processedMessage, "payload")
    }

    @Test
    fun testKeepNonexistentField() {
        val message = Message.of(
            "removed" to "Removed",
            "payload" to "We've got cats and dogs"
        )

        val processedMessage = processMessageWithConfig(message, "keep \$payload \$whatever")

        assertFieldNames(processedMessage, "payload")
    }

    @Test
    fun testKeepMultipleFields() {
        val message = Message.of(
            "removed" to "Removed",
            "kind" to "Kind",
            "payload" to "We've got cats and dogs"
        )

        val processedMessage = processMessageWithConfig(message, "keep \$payload \$kind")

        assertFieldNames(processedMessage, "kind", "payload")
    }
}
