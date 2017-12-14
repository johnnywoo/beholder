package ru.agalkin.beholder

import org.junit.Test
import ru.agalkin.beholder.config.commands.Arguments
import ru.agalkin.beholder.config.commands.CommandArguments
import ru.agalkin.beholder.config.commands.ParseCommand
import ru.agalkin.beholder.config.parser.ArgumentToken
import ru.agalkin.beholder.config.parser.LiteralToken
import ru.agalkin.beholder.config.parser.Token
import ru.agalkin.beholder.formatters.DumpFormatter
import kotlin.test.assertEquals

class SyslogParserTest {
    @Test
    fun testSyslogParser() {
        val message = Message()
        message["payload"] = "<190>Nov 25 13:46:44 vps nginx: 127.0.0.1 - - [25/Nov/2017:13:46:44 +0300] \"GET /api HTTP/1.1\" 200 47 \"-\" \"curl/7.38.0\""

        val parseCommand = ParseCommand(argumentsFromString("parse syslog"))
        // commands modify messages in place (messages are copied ahead of time by routers)
        parseCommand.receiveMessage(message)

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

    private fun argumentsFromString(command: String): Arguments {
        val tokens = Token.getTokens(command)
        val arguments = CommandArguments(tokens[0] as LiteralToken)
        for (token in tokens.drop(1)) {
            arguments.addToken(token as ArgumentToken)
        }
        return arguments
    }
}
