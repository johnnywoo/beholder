package ru.agalkin.beholder.passthrough

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.testutils.TestAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsonInflaterTest : TestAbstract() {
    @Test
    fun testJsonInflater() {
        val message = Message.of("payload" to """{"field":"value"}""")

        val processedMessage = processMessageWithConfig(message, "parse json")

        assertEquals(
            """
            |¥field=value
            |¥payload={"field":"value"}
            """.trimMargin().replace('¥', '$'),
            getMessageDump(processedMessage)
        )
    }

    @Test
    fun testJsonInflaterMultiple() {
        val message = Message.of("payload" to """{"field":"value","field2":"value2"}""")

        val processedMessage = processMessageWithConfig(message, "parse json")

        assertEquals(
            """
            |¥field=value
            |¥field2=value2
            |¥payload={"field":"value","field2":"value2"}
            """.trimMargin().replace('¥', '$'),
            getMessageDump(processedMessage)
        )
    }

    @Test
    fun testJsonInflaterNumber() {
        val message = Message.of("payload" to """{"field":123,"field2":0}""")

        val processedMessage = processMessageWithConfig(message, "parse json")

        assertEquals(
            """
            |¥field=123
            |¥field2=0
            |¥payload={"field":123,"field2":0}
            """.trimMargin().replace('¥', '$'),
            getMessageDump(processedMessage)
        )
    }

    @Test
    fun testJsonInflaterBoolean() {
        val message = Message.of("payload" to """{"field":true,"field2":false}""")

        val processedMessage = processMessageWithConfig(message, "parse json")

        assertEquals(
            """
            |¥field=true
            |¥field2=false
            |¥payload={"field":true,"field2":false}
            """.trimMargin().replace('¥', '$'),
            getMessageDump(processedMessage)
        )
    }

    @Test
    fun testJsonInflaterNull() {
        val message = Message.of(
            "field" to "To be removed",
            "payload" to """{"field":null}"""
        )

        val processedMessage = processMessageWithConfig(message, "parse json")

        assertEquals(
            """
            |¥payload={"field":null}
            """.trimMargin().replace('¥', '$'),
            getMessageDump(processedMessage)
        )
    }

    @Test
    fun testJsonInflaterParseError() {
        val message = Message.of("payload" to """{"field":""")

        val processedMessage = processMessageWithConfig(message, "parse json")

        assertNull(processedMessage)
    }

    @Test
    fun testJsonInflaterNotObject() {
        val message = Message.of("payload" to "[]")

        val processedMessage = processMessageWithConfig(message, "parse json")

        assertNull(processedMessage)
    }

    @Test
    fun testJsonInflaterNestedObject() {
        val message = Message.of("payload" to """{"a":{"b":"c"}}""")

        val processedMessage = processMessageWithConfig(message, "parse json")

        assertNull(processedMessage)
    }

    @Test
    fun testJsonInflaterNestedObjectAndOthers() {
        val message = Message.of("payload" to """{"z":"z","a":{"b":"c"}}""")

        val processedMessage = processMessageWithConfig(message, "parse json")

        assertNull(processedMessage)
    }

    @Test
    fun testJsonInflaterMultiline() {
        val message = Message.of("payload" to """{"payload":"Multiple\nlines"}""")

        val processedMessage = processMessageWithConfig(message, "parse json")

        assertEquals(
            "Multiple\nlines",
            processedMessage!!.getStringField("payload")
        )
    }

    @Test
    fun testJsonInflaterCodePoints() {
        val message = Message.of("payload" to """{"payload":"\u041c\u0430\u043c\u0430 \u043c\u044b\u043b\u0430 \u0440\u0430\u043c\u0443"}""")

        val processedMessage = processMessageWithConfig(message, "parse json")

        assertEquals(
            "Мама мыла раму",
            processedMessage!!.getStringField("payload")
        )
    }
}
