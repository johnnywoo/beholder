package ru.agalkin.beholder

import org.junit.Test
import ru.agalkin.beholder.commands.KeepCommand
import ru.agalkin.beholder.config.Config
import ru.agalkin.beholder.config.expressions.CommandArguments
import ru.agalkin.beholder.commands.SetCommand
import ru.agalkin.beholder.config.parser.ArgumentToken
import ru.agalkin.beholder.config.parser.LiteralToken
import ru.agalkin.beholder.config.parser.ParseException
import ru.agalkin.beholder.config.parser.Token
import kotlin.test.assertEquals
import kotlin.test.fail

class KeepTest {
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
        val command = getCommandFromString("keep \$payload")

        val message = Message()
        message["removed"] = "Removed"
        message["payload"] = "We've got cats and dogs"

        command.receiveMessage(message)

        assertEquals("payload", message.getFields().keys.joinToString { it })
    }

    @Test
    fun testKeepNonexistentField() {
        val command = getCommandFromString("keep \$payload \$whatever")

        val message = Message()
        message["removed"] = "Removed"
        message["payload"] = "We've got cats and dogs"

        command.receiveMessage(message)

        assertEquals("payload", message.getFields().keys.joinToString { it })
    }

    @Test
    fun testKeepMultipleFields() {
        val command = getCommandFromString("keep \$payload \$kind")

        val message = Message()
        message["removed"] = "Removed"
        message["kind"]    = "Kind"
        message["payload"] = "We've got cats and dogs"

        command.receiveMessage(message)

        assertEquals("kind,payload", message.getFields().keys.sorted().joinToString(",") { it })
    }

    private fun getCommandFromString(string: String): KeepCommand {
        val tokens = Token.getTokens(string)
        val args = CommandArguments(tokens[0] as LiteralToken)
        for (token in tokens.drop(1)) {
            args.addToken(token as ArgumentToken)
        }
        return KeepCommand(args)
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
