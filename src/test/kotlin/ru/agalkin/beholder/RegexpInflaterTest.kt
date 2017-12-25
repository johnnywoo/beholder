package ru.agalkin.beholder

import org.junit.Test
import ru.agalkin.beholder.commands.ParseCommand
import ru.agalkin.beholder.config.expressions.CommandArguments
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

        processMessageWithCommand(message, "parse ~(?<animal>cat|dog)~")

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

        processMessageWithCommand(message, "parse ~(?<animal>whale)~")

        assertEquals(
            "\$payload=We've got cats and dogs",
            getMessageDump(message)
        )
    }

    @Test
    fun testRegexpInflaterNumberedGroup() {
        val message = Message()
        message["payload"] = "We've got cats and dogs"

        processMessageWithCommand(message, "parse ~(cat)~")

        // there are no named groups, so nothing should change
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

        processMessageWithCommand(message, "parse ~(?<animal>whale)~")

        assertEquals(
            "\$payload=We've got cats and dogs\n" +
                "\$animal=headcrab",
            getMessageDump(message)
        )
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

    private fun getMessageDump(message: Message)
        = DumpFormatter().formatMessage(message).replace(Regex("^.*\n"), "")
}
