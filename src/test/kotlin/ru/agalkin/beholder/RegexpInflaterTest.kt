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

class RegexpInflaterTest {
    @Test
    fun testRegexpInflater() {
        val message = Message()
        message["payload"] = "We've got cats and dogs"

        val parseCommand = ParseCommand(argumentsFromString("parse /(?<animal>cat|dog)/"))
        // commands modify messages in place (messages are copied ahead of time by routers)
        parseCommand.receiveMessage(message)

        assertEquals(
            "\$payload=We've got cats and dogs\n" +
                "\$animal=cat",
            getMessageDump(message)
        )
    }

    @Test
    fun testRegexpInflaterNoMatch() {
        val message = Message()
        message["payload"] = "We've got cats and dogs"

        val parseCommand = ParseCommand(argumentsFromString("parse /(?<animal>whale)/"))
        // commands modify messages in place (messages are copied ahead of time by routers)
        parseCommand.receiveMessage(message)

        assertEquals(
            "\$payload=We've got cats and dogs",
            getMessageDump(message)
        )
    }

    @Test
    fun testRegexpInflaterNoMatchNoOverwrite() {
        val message = Message()
        message["payload"] = "We've got cats and dogs"
        message["animal"]  = "headcrab"

        val parseCommand = ParseCommand(argumentsFromString("parse /(?<animal>whale)/"))
        // commands modify messages in place (messages are copied ahead of time by routers)
        parseCommand.receiveMessage(message)

        assertEquals(
            "\$payload=We've got cats and dogs\n" +
                "\$animal=headcrab",
            getMessageDump(message)
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

    private fun getMessageDump(message: Message)
        = DumpFormatter().formatMessage(message).replace(Regex("^.*\n"), "")
}
