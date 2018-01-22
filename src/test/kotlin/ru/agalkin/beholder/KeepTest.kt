package ru.agalkin.beholder

import org.junit.Test
import kotlin.test.assertEquals

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
        assertConfigFails("keep", "`keep` needs at least one field name: keep")
    }

    @Test
    fun testKeepFailsStringArg() {
        assertConfigFails("keep \$a 'b'", "All arguments of `keep` must be field names: keep \$a 'b'")
    }

    @Test
    fun testKeepFailsRegexpArg() {
        assertConfigFails("keep \$a ~b~", "All arguments of `keep` must be field names: keep \$a ~b~")
    }

    @Test
    fun testKeepWorks() {
        val message = Message()
        message["removed"] = "Removed"
        message["payload"] = "We've got cats and dogs"

        val processedMessage = processMessageWithCommand(message, "keep \$payload")

        assertEquals("payload", processedMessage!!.getFields().keys.joinToString { it })
    }

    @Test
    fun testKeepNonexistentField() {
        val message = Message()
        message["removed"] = "Removed"
        message["payload"] = "We've got cats and dogs"

        val processedMessage = processMessageWithCommand(message, "keep \$payload \$whatever")

        assertEquals("payload", processedMessage!!.getFields().keys.joinToString { it })
    }

    @Test
    fun testKeepMultipleFields() {
        val message = Message()
        message["removed"] = "Removed"
        message["kind"]    = "Kind"
        message["payload"] = "We've got cats and dogs"

        val processedMessage = processMessageWithCommand(message, "keep \$payload \$kind")

        assertEquals("kind,payload", processedMessage!!.getFields().keys.sorted().joinToString(",") { it })
    }
}