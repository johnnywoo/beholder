package ru.agalkin.beholder

import org.junit.Test
import ru.agalkin.beholder.config.Config
import ru.agalkin.beholder.config.expressions.CommandArguments
import ru.agalkin.beholder.commands.SetCommand
import ru.agalkin.beholder.config.parser.ArgumentToken
import ru.agalkin.beholder.config.parser.LiteralToken
import ru.agalkin.beholder.config.parser.ParseException
import ru.agalkin.beholder.config.parser.Token
import kotlin.test.assertEquals
import kotlin.test.fail

class ReplaceTest {
    @Test
    fun testRegexParses() {
        assertConfigParses(
            """
            |set ¥payload replace ~cat~ 'dog';
            |""".trimMargin().replace('¥', '$'),
            """
            |set ¥payload replace ~cat~ 'dog';
            |""".trimMargin().replace('¥', '$')
        )
    }

    @Test
    fun testReplaceBad() {
        assertConfigFails(
            """
            |set ¥payload replace 'cat' 'dog';
            |""".trimMargin().replace('¥', '$'),
            "`replace` needs a regexp: set \$payload replace 'cat' 'dog'"
        )
    }

    @Test
    fun testReplaceBadRegexModifier() {
        assertConfigFails(
            """
            |set ¥payload replace ~cat~q 'dog';
            |""".trimMargin().replace('¥', '$'),
            "Invalid regexp modifier: q"
        )
    }

    @Test
    fun testReplaceGoodRegexModifier() {
        assertConfigParses(
            """
            |set ¥payload replace ~cat~i 'dog';
            |""".trimMargin().replace('¥', '$'),
            """
            |set ¥payload replace ~cat~i 'dog';
            |""".trimMargin().replace('¥', '$')
        )
    }

    @Test
    fun testReplaceBadRegexReplacement() {
        assertConfigParses(
            """
            |set ¥payload replace ~cat~ '$1';
            |""".trimMargin().replace('¥', '$'),
            """
            |set ¥payload replace ~cat~ '$1';
            |""".trimMargin().replace('¥', '$')
        )

        val setCommand = getCommandFromString("set \$payload replace ~cat~ 'huge \$1'")

        val message = Message()
        message["payload"] = "We've got a cat here"

        setCommand.receiveMessage(message)

        // текст не изменился, команда не упала с исключением
        assertEquals("We've got a cat here", message.getStringField("payload"))
    }

    @Test
    fun testReplaceWorks() {
        val setCommand = getCommandFromString("set \$payload replace ~cat|dog~ animal")

        val message = Message()
        message["payload"] = "We've got cats and dogs"

        setCommand.receiveMessage(message)

        assertEquals("We've got animals and animals", message.getStringField("payload"))
    }

    @Test
    fun testReplaceNewlines() {
        val setCommand = getCommandFromString("""set ¥payload replace ~\n~ '\\\\n'""".replace('¥', '$'))

        val message = Message()
        message["payload"] = "Line 1\nLine 2"

        setCommand.receiveMessage(message)

        assertEquals("""Line 1\nLine 2""", message.getStringField("payload"))
    }

    @Test
    fun testReplaceInterpolationSimple() {
        val setCommand = getCommandFromString("set \$payload replace ~cat~ '\$animal'")

        val message = Message()
        message["animal"]  = "feline"
        message["payload"] = "We've got cats and dogs"

        setCommand.receiveMessage(message)

        assertEquals("""We've got felines and dogs""", message.getStringField("payload"))
    }

    @Test
    fun testReplaceInterpolationAndGroups() {
        val setCommand = getCommandFromString("set \$payload replace ~(cat|dog)~ '\$size \$1'")

        val message = Message()
        message["size"]    = "huge"
        message["payload"] = "We've got cats and dogs"

        setCommand.receiveMessage(message)

        assertEquals("""We've got huge cats and huge dogs""", message.getStringField("payload"))
    }

    @Test
    fun testReplaceWithEmptyString() {
        val setCommand = getCommandFromString("set \$payload replace ~cats and ~ ''")

        val message = Message()
        message["payload"] = "We've got cats and dogs"

        setCommand.receiveMessage(message)

        assertEquals("""We've got dogs""", message.getStringField("payload"))
    }

    @Test
    fun testReplaceWithUnknownField() {
        val setCommand = getCommandFromString("set \$payload replace ~cats and ~ \$unknown")

        val message = Message()
        message["payload"] = "We've got cats and dogs"

        setCommand.receiveMessage(message)

        assertEquals("""We've got dogs""", message.getStringField("payload"))
    }

    @Test
    fun testReplaceInUnknownField() {
        val setCommand = getCommandFromString("set \$payload replace ~cats and ~ '' in \$unknown")

        val message = Message()
        message["payload"] = "We've got cats and dogs"

        setCommand.receiveMessage(message)

        assertEquals("", message.getStringField("payload"))
    }

    @Test
    fun testReplaceIn() {
        val message = Message()
        message["payload"] = "To be ignored"

        val setCommand = getCommandFromString("set \$payload replace ~cat~ 'animal' in 'Zoo has cats'")
        setCommand.receiveMessage(message)

        assertEquals("""Zoo has animals""", message.getStringField("payload"))
    }

    @Test
    fun testReplaceInField() {
        val message = Message()
        message["text"]    = "Zoo has cats"
        message["payload"] = "To be ignored"

        val setCommand = getCommandFromString("set \$payload replace ~cat~ 'animal' in \$text")
        setCommand.receiveMessage(message)

        assertEquals("""Zoo has animals""", message.getStringField("payload"))
    }

    @Test
    fun testReplaceInWithFields() {
        val message = Message()
        message["animal"]  = "cat"
        message["payload"] = "To be ignored"

        val setCommand = getCommandFromString("set \$payload replace ~cat~ 'feline' in 'Two cats: \$animal \$animal'")
        setCommand.receiveMessage(message)

        assertEquals("""Two felines: feline feline""", message.getStringField("payload"))
    }

    @Test
    fun testReplaceInWithFieldsEverywhere() {
        val message = Message()
        message["animal"]     = "cat"
        message["betterName"] = "feline"
        message["payload"]    = "To be ignored"

        val setCommand = getCommandFromString("set \$payload replace ~cat~ \$betterName in 'Two cats: \$animal \$animal'")
        setCommand.receiveMessage(message)

        assertEquals("""Two felines: feline feline""", message.getStringField("payload"))
    }

    @Test
    fun testReplaceFromHelp1() {
        val message = Message()
        message["payload"] = "127.0.0.1 WARN PHP Warning: some warning"

        val setCommand = getCommandFromString("set \$payload replace ~warn(ing)?~i 'WARNING'")
        setCommand.receiveMessage(message)

        assertEquals("127.0.0.1 WARNING PHP WARNING: some WARNING", message.getStringField("payload"))
    }

    @Test
    fun testReplaceFromHelp2() {
        val message = Message()
        message["subdomain"] = "www"
        message["domain"]    = "example.com"

        val setCommand = getCommandFromString("set \$host replace ~^www\\.~ '' in '\$subdomain.\$domain'")
        setCommand.receiveMessage(message)

        assertEquals("example.com", message.getStringField("host"))

        val message2 = Message()
        message2["subdomain"] = "mail"
        message2["domain"]    = "example.com"

        setCommand.receiveMessage(message2)

        assertEquals("mail.example.com", message2.getStringField("host"))
    }

    @Test
    fun testReplaceFromHelp3() {
        val message = Message()
        message["payload"] = "a\nb"

        val setCommand = getCommandFromString("set \$payload replace ~\\n~ '\\\\\\\\n'")
        setCommand.receiveMessage(message)

        assertEquals("a\\nb", message.getStringField("payload"))
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
