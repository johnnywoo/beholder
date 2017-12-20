package ru.agalkin.beholder

import org.junit.Test
import ru.agalkin.beholder.config.Config
import ru.agalkin.beholder.config.parser.ParseException
import ru.agalkin.beholder.config.parser.Token
import kotlin.test.assertEquals
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
    fun testFromTimerDefault() {
        assertConfigParses(
            "from timer;",
            """
            |from timer;
            |""".trimMargin()
        )
    }

    @Test
    fun testFromTimerOptionalArgument() {
        assertConfigParses(
            "from timer 1 second;",
            """
            |from timer 1 second;
            |""".trimMargin()
        )
    }

    @Test
    fun testFromTimerError() {
        assertConfigFails(
            "from timer n second;",
            "Correct syntax is `from timer 10 seconds`: from timer n second"
        )
    }

    @Test
    fun testFromTimerSuffixError() {
        assertConfigFails(
            "from timer 1 lightyear;",
            "Too many arguments for `from`: from timer 1 lightyear"
        )
    }

    @Test
    fun testFromTimerExtraTokenAfterOptional() {
        assertConfigFails(
            "from timer 1 second foo;",
            "Too many arguments for `from`: from timer 1 second foo"
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
    fun testBadPort() {
        assertConfigFails(
            "from udp wtf;",
            "Invalid network address 'wtf': from udp wtf"
        )
    }

    @Test
    fun testUnclosedQuotes() {
        assertConfigFails(
            "set \$f 'bla",
            "Unclosed string literal detected: 'bla"
        )
    }

    @Test
    fun testQuotesLast() {
        assertConfigParses(
            "set \$f 'bla'",
            "set \$f 'bla';\n"
        )
    }

    @Test
    fun testUnclosedRegexp() {
        assertConfigFails(
            "set \$f replace /bla",
            "Unclosed regexp detected: /bla"
        )
    }

    @Test
    fun testRegexpLast() {
        // проверяем, что когда у нас regexp последний токен, мы определяем корректно, что он закрылся
        // при этом в команде set будет ошибка, что не хватает аргументов (нет строки замены)
        assertConfigFails(
            "set \$f replace /bla/",
            "`replace` needs a replacement string: set \$f replace /bla/"
        )
    }

    @Test
    fun testRegexpInvalid() {
        assertConfigFails(
            "set \$f replace /+/",
            "Invalid regexp: Dangling meta character '+' near index 0\n+\n^"
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
