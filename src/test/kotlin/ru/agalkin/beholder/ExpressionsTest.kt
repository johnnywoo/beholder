package ru.agalkin.beholder

import org.junit.Test
import ru.agalkin.beholder.config.Config
import ru.agalkin.beholder.config.commands.CommandArguments
import ru.agalkin.beholder.config.commands.SetCommand
import ru.agalkin.beholder.config.parser.ArgumentToken
import ru.agalkin.beholder.config.parser.LiteralToken
import ru.agalkin.beholder.config.parser.ParseException
import ru.agalkin.beholder.config.parser.Token
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

class ExpressionsTest {
    @Test
    fun testFlowEmpty() {
        assertConfigParses(
            "flow { }",
            """
            |flow;
            |""".trimMargin()
        )
    }

    @Test
    fun testFlowNoSpaces() {
        assertConfigParses(
            "flow{}",
            """
            |flow;
            |""".trimMargin()
        )
    }

    @Test
    fun testFlowSimple() {
        assertConfigParses(
            "flow {from timer}",
            """
            |flow {
            |    from timer;
            |}
            |""".trimMargin()
        )
    }

    @Test
    fun testUnknownCommand() {
        assertEquals(
            """
            |LiteralToken unknown-command
            |""".trimMargin(),
            dumpTokens("unknown-command")
        )
        assertConfigFails(
            "unknown-command",
            "Command `unknown-command` is not allowed inside RootCommand: unknown-command"
        )
    }

    @Test
    fun testUnknownCommandSemicolon() {
        assertEquals(
            """
            |LiteralToken unknown-command
            |SemicolonToken ;
            |""".trimMargin(),
            dumpTokens("unknown-command;")
        )
        assertConfigFails(
            "unknown-command;",
            "Command `unknown-command` is not allowed inside RootCommand: unknown-command"
        )
    }

    @Test
    fun testFlowMultiple() {
        assertConfigParses(
            """
            |flow {
            |    flow {
            |        from timer;
            |        set ¥payload '¥receivedDate ¥payload';
            |        to stdout;
            |    }
            |    flow {
            |        from timer;
            |        set ¥payload '¥receivedDate ¥payload';
            |        to stdout;
            |    }
            |}
            |""".trimMargin().replace('¥', '$'),
            """
            |flow {
            |    flow {
            |        from timer;
            |        set ¥payload '¥receivedDate ¥payload';
            |        to stdout;
            |    }
            |    flow {
            |        from timer;
            |        set ¥payload '¥receivedDate ¥payload';
            |        to stdout;
            |    }
            |}
            |""".trimMargin().replace('¥', '$')
        )
    }

    @Test
    fun testFlowRoot() {
        assertConfigParses(
            """
            |from timer;
            |set ¥payload '¥receivedDate ¥payload';
            |to stdout;
            |""".trimMargin().replace('¥', '$'),
            """
            |from timer;
            |set ¥payload '¥receivedDate ¥payload';
            |to stdout;
            |""".trimMargin().replace('¥', '$')
        )
    }

    @Test
    fun testRegexParses() {
        assertConfigParses(
            """
            |set ¥payload replace /cat/ 'dog';
            |""".trimMargin().replace('¥', '$'),
            """
            |set ¥payload replace /cat/ 'dog';
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
    fun testReplaceWorks() {
        val tokens = Token.getTokens("set \$payload replace /cat|dog/ animal")
        val args = CommandArguments(tokens[0] as LiteralToken)
        for (token in tokens.drop(1)) {
            args.add(token as ArgumentToken)
        }
        val setCommand = SetCommand(args)

        val message = Message()
        message["payload"] = "We've got cats and dogs"

        setCommand.receiveMessage(message)

        assertEquals("We've got animals and animals", message["payload"])
    }

    @Test
    fun testReplaceNewlines() {
        val tokens = Token.getTokens("""set ¥payload replace /\n/ '\\\\n'""".replace('¥', '$'))
        val args = CommandArguments(tokens[0] as LiteralToken)
        for (token in tokens.drop(1)) {
            args.add(token as ArgumentToken)
        }
        val setCommand = SetCommand(args)

        val message = Message()
        message["payload"] = "Line 1\nLine 2"

        setCommand.receiveMessage(message)

        assertEquals("""Line 1\nLine 2""", message["payload"])
    }

    private fun dumpTokens(configText: String): String {
        val tokens = Token.getTokens(configText)
        val sb = StringBuilder()
        for (token in tokens) {
            sb.append(token::class.simpleName)
            sb.append(' ')
            sb.append(token.getDefinition())
            sb.append('\n')
        }
        return sb.toString()
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
