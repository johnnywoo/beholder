package ru.agalkin.beholder

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.Test
import kotlin.test.assertEquals

class JsonTest : TestAbstract() {
    @Test
    fun testJsonParses() {
        assertConfigParses("set \$json json", "set \$json json;\n")
    }

    @Test
    fun testJsonParsesSingle() {
        assertConfigParses("set \$json json \$a", "set \$json json \$a;\n")
    }

    @Test
    fun testJsonParsesMultiple() {
        assertConfigParses("set \$json json \$a \$b", "set \$json json \$a \$b;\n")
    }

    @Test
    fun testJsonFailsStringArg() {
        assertConfigFails("set \$json json \$a 'b'", "`set ... json` arguments must be field names: set \$json json \$a 'b' [test-config:1]")
    }

    @Test
    fun testJsonFailsRegexpArg() {
        assertConfigFails("set \$json json \$a ~b~", "`set ... json` arguments must be field names: set \$json json \$a ~b~ [test-config:1]")
    }

    @Test
    fun testJsonWorks() {
        val message = Message()
        message["custom"]  = "Custom"
        message["payload"] = "We've got cats and dogs"

        val processedMessage = processMessageWithConfig(message, "set \$json json")

        assertEquals("""{"custom":"Custom","payload":"We've got cats and dogs"}""", processedMessage!!.getStringField("json"))
    }

    @Test
    fun testJsonMultiline() {
        val message = Message()
        message["custom"]  = "Custom\nMore custom"
        message["payload"] = "We've got cats and dogs"

        val processedMessage = processMessageWithConfig(message, "set \$json json")

        assertEquals("""{"custom":"Custom\nMore custom","payload":"We've got cats and dogs"}""", processedMessage!!.getStringField("json"))
    }

    @Test
    fun testJsonNonexistentField() {
        val message = Message()
        message["ignored"] = "Ignored"
        message["payload"] = "We've got cats and dogs"

        val processedMessage = processMessageWithConfig(message, "set \$json json \$payload \$whatever")

        assertEquals("""{"payload":"We've got cats and dogs","whatever":""}""", processedMessage!!.getStringField("json"))
    }

    @Test
    fun testJsonCyrillic() {
        val message = Message()
        message["payload"] = "Мама мыла раму"

        val processedMessage = processMessageWithConfig(message, "set \$json json")

        assertEquals("""{"payload":"Мама мыла раму"}""", processedMessage!!.getStringField("json"))
    }

    @Test
    fun testJsonMultiCodePoint() {
        val message = Message()
        message["payload"] = "\uD83D\uDCA9"

        val processedMessage = processMessageWithConfig(message, "set \$json json")

        assertEquals("""{"payload":"💩"}""", processedMessage!!.getStringField("json"))

        // we want to verify our payload can be extracted from the json string
        val json = Gson().fromJson(processedMessage.getStringField("json"), JsonObject::class.java)
        assertEquals(processedMessage.getStringField("payload"), json.get("payload").asString ?: "???")
    }
}
