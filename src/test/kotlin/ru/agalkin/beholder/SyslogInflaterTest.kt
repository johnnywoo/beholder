package ru.agalkin.beholder

import org.junit.Test
import ru.agalkin.beholder.config.commands.CommandArguments
import ru.agalkin.beholder.config.commands.ParseCommand
import ru.agalkin.beholder.config.parser.ArgumentToken
import ru.agalkin.beholder.config.parser.LiteralToken
import ru.agalkin.beholder.config.parser.Token
import ru.agalkin.beholder.formatters.DumpFormatter
import kotlin.test.assertEquals

class SyslogInflaterTest {
    @Test
    fun testSyslogInflater() {
        val message = Message()
        message["payload"] = "<190>Nov 25 13:46:44 vps nginx: 127.0.0.1 - - [25/Nov/2017:13:46:44 +0300] \"GET /api HTTP/1.1\" 200 47 \"-\" \"curl/7.38.0\""

        processMessageWithCommand(message, "parse syslog")

        assertEquals(
            """
                |¥payload=127.0.0.1 - - [25/Nov/2017:13:46:44 +0300] "GET /api HTTP/1.1" 200 47 "-" "curl/7.38.0"
                |¥syslogFacility=23
                |¥syslogSeverity=6
                |¥syslogHost=vps
                |¥syslogProgram=nginx
                """.trimMargin().replace('¥', '$'),
            DumpFormatter().formatMessage(message).replace(Regex("^.*\n"), "")
        )
    }

    @Test
    fun testSyslogInflaterMultiline() {
        val message = Message()
        message["payload"] = "<190>Nov 25 13:46:44 vps nginx: a\nb"

        processMessageWithCommand(message, "parse syslog")

        assertEquals("a\nb", message.getPayload())
    }

    private fun processMessageWithCommand(message: Message, command: String) {
        val tokens = Token.getTokens(command)
        val arguments = CommandArguments(tokens[0] as LiteralToken)
        for (token in tokens.drop(1)) {
            arguments.addToken(token as ArgumentToken)
        }

        // commands modify messages in place (messages are copied ahead of time by routers)
        ParseCommand(arguments).receiveMessage(message)
    }
}
