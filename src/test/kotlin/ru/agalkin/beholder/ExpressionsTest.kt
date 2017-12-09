package ru.agalkin.beholder

import org.junit.Test
import ru.agalkin.beholder.config.Config
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
