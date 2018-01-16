package ru.agalkin.beholder

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.Test
import ru.agalkin.beholder.commands.SetCommand
import ru.agalkin.beholder.config.Config
import ru.agalkin.beholder.config.expressions.CommandArguments
import ru.agalkin.beholder.config.parser.ArgumentToken
import ru.agalkin.beholder.config.parser.LiteralToken
import ru.agalkin.beholder.config.parser.ParseException
import ru.agalkin.beholder.config.parser.Token
import kotlin.test.assertEquals
import kotlin.test.fail

class JsonTest {
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
        assertConfigFails("set \$json json \$a 'b'", "`set ... json` arguments must be field names: set \$json json \$a 'b'")
    }

    @Test
    fun testJsonFailsRegexpArg() {
        assertConfigFails("set \$json json \$a ~b~", "`set ... json` arguments must be field names: set \$json json \$a ~b~")
    }

    @Test
    fun testJsonWorks() {
        val command = getCommandFromString("set \$json json")

        val message = Message()
        message["custom"]  = "Custom"
        message["payload"] = "We've got cats and dogs"

        command.receiveMessage(message)

        assertEquals("""{"custom":"Custom","payload":"We've got cats and dogs"}""", message.getStringField("json"))
    }

    @Test
    fun testJsonMultiline() {
        val command = getCommandFromString("set \$json json")

        val message = Message()
        message["custom"]  = "Custom\nMore custom"
        message["payload"] = "We've got cats and dogs"

        command.receiveMessage(message)

        assertEquals("""{"custom":"Custom\nMore custom","payload":"We've got cats and dogs"}""", message.getStringField("json"))
    }

    @Test
    fun testJsonNonexistentField() {
        val command = getCommandFromString("set \$json json \$payload \$whatever")

        val message = Message()
        message["ignored"] = "Ignored"
        message["payload"] = "We've got cats and dogs"

        command.receiveMessage(message)

        assertEquals("""{"payload":"We've got cats and dogs","whatever":""}""", message.getStringField("json"))
    }

    @Test
    fun testJsonCyrillic() {
        val command = getCommandFromString("set \$json json")

        val message = Message()
        message["payload"] = "Мама мыла раму"

        command.receiveMessage(message)

        assertEquals("""{"payload":"Мама мыла раму"}""", message.getStringField("json"))
    }

    @Test
    fun testJsonMultiCodePoint() {
        val command = getCommandFromString("set \$json json")

        val message = Message()
        message["payload"] = "\uD83D\uDCA9"

        command.receiveMessage(message)

        assertEquals("""{"payload":"💩"}""", message.getStringField("json"))

        // we want to verify our payload can be extracted from the json string
        val json = Gson().fromJson(message.getStringField("json"), JsonObject::class.java)
        assertEquals(message.getStringField("payload"), json.get("payload").asString ?: "???")
    }

    private fun getCommandFromString(string: String): SetCommand {
        val tokens = Token.getTokens(string)
        val args = CommandArguments(tokens[0] as LiteralToken)
        for (token in tokens.drop(1)) {
            args.addToken(token as ArgumentToken)
        }
        return SetCommand(args)
    }

    private fun assertConfigParses(fromText: String, toDefinition: String) {
        assertEquals(toDefinition, Config(fromText).getDefinition())
    }

    private fun assertConfigFails(fromText: String, errorMessage: String) {
        try {
            val definition = Config(fromText).getDefinition()
            fail("This config should not parse correctly: $fromText\n=== parsed ===\n$definition\n===")
        } catch (e: ParseException) {
            assertEquals(errorMessage, e.message)
        }
    }
}
