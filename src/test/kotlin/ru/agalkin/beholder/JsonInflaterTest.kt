package ru.agalkin.beholder

import org.junit.Test
import ru.agalkin.beholder.formatters.DumpFormatter
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsonInflaterTest : TestAbstract() {
    @Test
    fun testJsonInflater() {
        val message = Message()
        message["payload"] = """{"field":"value"}"""

        val processedMessage = processMessageWithCommand(message, "parse json")

        assertEquals(
            """
                |¥payload={"field":"value"}
                |¥field=value
                """.trimMargin().replace('¥', '$'),
            DumpFormatter().formatMessage(processedMessage!!).toString().replace(Regex("^.*\n"), "")
        )
    }

    @Test
    fun testJsonInflaterMultiple() {
        val message = Message()
        message["payload"] = """{"field":"value","field2":"value2"}"""

        val processedMessage = processMessageWithCommand(message, "parse json")

        assertEquals(
            """
                |¥payload={"field":"value","field2":"value2"}
                |¥field=value
                |¥field2=value2
                """.trimMargin().replace('¥', '$'),
            DumpFormatter().formatMessage(processedMessage!!).toString().replace(Regex("^.*\n"), "")
        )
    }

    @Test
    fun testJsonInflaterNumber() {
        val message = Message()
        message["payload"] = """{"field":123,"field2":0}"""

        val processedMessage = processMessageWithCommand(message, "parse json")

        assertEquals(
            """
                |¥payload={"field":123,"field2":0}
                |¥field=123
                |¥field2=0
                """.trimMargin().replace('¥', '$'),
            DumpFormatter().formatMessage(processedMessage!!).toString().replace(Regex("^.*\n"), "")
        )
    }

    @Test
    fun testJsonInflaterBoolean() {
        val message = Message()
        message["payload"] = """{"field":true,"field2":false}"""

        val processedMessage = processMessageWithCommand(message, "parse json")

        assertEquals(
            """
                |¥payload={"field":true,"field2":false}
                |¥field=true
                |¥field2=false
                """.trimMargin().replace('¥', '$'),
            DumpFormatter().formatMessage(processedMessage!!).toString().replace(Regex("^.*\n"), "")
        )
    }

    @Test
    fun testJsonInflaterNull() {
        val message = Message()
        message["field"]   = "To be removed"
        message["payload"] = """{"field":null}"""

        val processedMessage = processMessageWithCommand(message, "parse json")

        assertEquals(
            """
                |¥payload={"field":null}
                """.trimMargin().replace('¥', '$'),
            DumpFormatter().formatMessage(processedMessage!!).toString().replace(Regex("^.*\n"), "")
        )
    }

    @Test
    fun testJsonInflaterParseError() {
        val message = Message()
        message["payload"] = """{"field":"""

        val processedMessage = processMessageWithCommand(message, "parse json")

        assertNull(processedMessage)
    }

    @Test
    fun testJsonInflaterNotObject() {
        val message = Message()
        message["payload"] = """[]"""

        val processedMessage = processMessageWithCommand(message, "parse json")

        assertNull(processedMessage)
    }

    @Test
    fun testJsonInflaterNestedObject() {
        val message = Message()
        message["payload"] = """{"a":{"b":"c"}}"""

        val processedMessage = processMessageWithCommand(message, "parse json")

        assertNull(processedMessage)
    }

    @Test
    fun testJsonInflaterNestedObjectAndOthers() {
        val message = Message()
        message["payload"] = """{"z":"z","a":{"b":"c"}}"""

        val processedMessage = processMessageWithCommand(message, "parse json")

        assertNull(processedMessage)
    }

    @Test
    fun testJsonInflaterMultiline() {
        val message = Message()
        message["payload"] = """{"payload":"Multiple\nlines"}"""

        val processedMessage = processMessageWithCommand(message, "parse json")

        assertEquals(
            "Multiple\nlines",
            processedMessage!!.getStringField("payload")
        )
    }

    @Test
    fun testJsonInflaterCodePoints() {
        val message = Message()
        message["payload"] = """{"payload":"\u041c\u0430\u043c\u0430 \u043c\u044b\u043b\u0430 \u0440\u0430\u043c\u0443"}"""

        val processedMessage = processMessageWithCommand(message, "parse json")

        assertEquals(
            "Мама мыла раму",
            processedMessage!!.getStringField("payload")
        )
    }
}
