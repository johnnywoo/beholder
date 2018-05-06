package ru.agalkin.beholder

import org.junit.Test
import ru.agalkin.beholder.config.parser.Token
import kotlin.test.assertEquals

class ExpressionsTest : TestAbstract() {
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
            "Correct syntax is `from timer 10 seconds`: from timer n second [test-config:1]"
        )
    }

    @Test
    fun testFromTimerSuffixError() {
        assertConfigFails(
            "from timer 1 lightyear;",
            "Too many arguments for `from`: from timer 1 lightyear [test-config:1]"
        )
    }

    @Test
    fun testFromTimerExtraTokenAfterOptional() {
        assertConfigFails(
            "from timer 1 second foo;",
            "Too many arguments for `from`: from timer 1 second foo [test-config:1]"
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
            "Command `unknown-command` is not allowed inside RootCommand: unknown-command [test-config:1]"
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
            "Command `unknown-command` is not allowed inside RootCommand: unknown-command [test-config:1]"
        )
    }

    @Test
    fun testFlowMultiple() {
        assertConfigParses(
            """
            |flow {
            |    flow {
            |        from timer;
            |        set ¥payload '¥date ¥payload';
            |        to stdout;
            |    }
            |    flow {
            |        from timer;
            |        set ¥payload '¥date ¥payload';
            |        to stdout;
            |    }
            |}
            |""".trimMargin().replace('¥', '$'),
            """
            |flow {
            |    flow {
            |        from timer;
            |        set ¥payload '¥date ¥payload';
            |        to stdout;
            |    }
            |    flow {
            |        from timer;
            |        set ¥payload '¥date ¥payload';
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
            |set ¥payload '¥date ¥payload';
            |to stdout;
            |""".trimMargin().replace('¥', '$'),
            """
            |from timer;
            |set ¥payload '¥date ¥payload';
            |to stdout;
            |""".trimMargin().replace('¥', '$')
        )
    }

    @Test
    fun testBadPort() {
        assertConfigFails(
            "from udp wtf;",
            "Invalid network address 'wtf': from udp wtf [test-config:1]"
        )
    }

    @Test
    fun testBadAddressWithFields() {
        assertConfigFails(
            "from udp 127.0.0.1:\$port;",
            "`from udp` needs at least a port number (message fields are not allowed here): from udp 127.0.0.1:\$port [test-config:1]"
        )
    }

    @Test
    fun testBadAddressWithFieldsQuoted() {
        assertConfigFails(
            "from udp '127.0.0.1:\$port';",
            "`from udp` needs at least a port number (message fields are not allowed here): from udp '127.0.0.1:\$port' [test-config:1]"
        )
    }

    @Test
    fun testUnclosedQuotes() {
        assertConfigFails(
            "set \$f 'bla",
            "Unclosed string literal detected: 'bla [test-config:1]"
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
            "set \$f replace ~bla",
            "Unclosed regexp detected: ~bla [test-config:1]"
        )
    }

    @Test
    fun testRegexpLast() {
        // проверяем, что когда у нас regexp последний токен, мы определяем корректно, что он закрылся
        // при этом в команде set будет ошибка, что не хватает аргументов (нет строки замены)
        assertConfigFails(
            "set \$f replace ~bla~",
            "`replace` needs a replacement string: set \$f replace ~bla~ [test-config:1]"
        )
    }

    @Test
    fun testRegexpInvalid() {
        assertConfigFails(
            "set \$f replace ~+~",
            "Invalid regexp: Dangling meta character '+' near index 0\n+\n^ [test-config:1]"
        )
    }

    @Test
    fun testLiteralField() {
        val message = Message()
        message["cat"] = "a gray cat"

        val processedMessage = processMessageWithCommand(message, "set \$result replace ~cat~ 'dog' in \$cat")
        assertEquals("a gray dog", processedMessage?.getStringField("result"))
    }

    @Test
    fun testQuotedField() {
        val message = Message()
        message["cat"] = "a gray cat"

        val processedMessage = processMessageWithCommand(message, "set \$result replace ~cat~ 'dog' in '\$cat'")
        assertEquals("a gray dog", processedMessage?.getStringField("result"))
    }

    @Test
    fun testDoubleQuotedField() {
        val message = Message()
        message["cat"] = "a gray cat"

        val processedMessage = processMessageWithCommand(message, "set \$result replace ~cat~ 'dog' in \"\$cat\"")
        assertEquals("a gray dog", processedMessage?.getStringField("result"))
    }

    @Test
    fun testCurlyBracedLiteralFieldFails() {
        assertConfigFails(
            "set \$result replace ~cat~ 'dog' in {\$cat}",
            "`replace ... in` needs a string: set \$result replace ~cat~ 'dog' in [test-config:1]"
        )
    }

    @Test
    fun testCurlyBracedQuotedField() {
        val message = Message()
        message["cat"] = "a gray cat"

        val processedMessage = processMessageWithCommand(message, "set \$result replace ~cat~ 'dog' in '{\$cat}'")
        assertEquals("a gray dog", processedMessage?.getStringField("result"))
    }

    @Test
    fun testCurlyBracedDoubleQuotedField() {
        val message = Message()
        message["cat"] = "a gray cat"

        val processedMessage = processMessageWithCommand(message, "set \$result replace ~cat~ 'dog' in \"{\$cat}\"")
        assertEquals("a gray dog", processedMessage?.getStringField("result"))
    }

    @Test
    fun testCurlyBracedFieldWithRemainder() {
        val message = Message()
        message["cat"] = "a gray cat"

        val processedMessage = processMessageWithCommand(message, "set \$result replace ~cat~ 'dog' in '{\$cat}astrophe'")
        assertEquals("a gray dogastrophe", processedMessage?.getStringField("result"))
    }

    private fun dumpTokens(configText: String): String {
        val tokens = Token.getTokens(configText, "test-config")
        val sb = StringBuilder()
        for (token in tokens) {
            sb.append(token::class.simpleName)
            sb.append(' ')
            sb.append(token.getDefinition())
            sb.append('\n')
        }
        return sb.toString()
    }
}
