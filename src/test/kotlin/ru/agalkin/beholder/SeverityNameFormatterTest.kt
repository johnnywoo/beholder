package ru.agalkin.beholder

import org.junit.Test
import kotlin.test.assertEquals

class SeverityNameFormatterTest : TestAbstract() {
    @Test
    fun testSeverityNameFormatter() {
        val message = Message()
        message["severity"] = "6"

        val parsedMessage = processMessageWithCommand(message, "set \$name severity-name \$severity")

        assertEquals("INFO", parsedMessage?.getStringField("name"))
    }

    @Test
    fun testSeverityNameFormatterLowercase() {
        val message = Message()
        message["severity"] = "6"

        val parsedMessage = processMessageWithCommand(message, "set \$name severity-name \$severity lowercase")

        assertEquals("info", parsedMessage?.getStringField("name"))
    }
}