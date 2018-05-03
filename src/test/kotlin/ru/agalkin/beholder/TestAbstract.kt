package ru.agalkin.beholder

import org.junit.BeforeClass
import ru.agalkin.beholder.commands.KeepCommand
import ru.agalkin.beholder.commands.ParseCommand
import ru.agalkin.beholder.commands.SetCommand
import ru.agalkin.beholder.config.Config
import ru.agalkin.beholder.config.expressions.CommandArguments
import ru.agalkin.beholder.config.expressions.RootCommand
import ru.agalkin.beholder.config.parser.ArgumentToken
import ru.agalkin.beholder.config.parser.LiteralToken
import ru.agalkin.beholder.config.parser.ParseException
import ru.agalkin.beholder.config.parser.Token
import ru.agalkin.beholder.formatters.DumpFormatter
import kotlin.test.assertEquals
import kotlin.test.fail

abstract class TestAbstract {
    protected fun processMessageWithCommand(message: Message, command: String): Message? {
        val tokens = Token.getTokens(command, "test-config")
        val arguments = CommandArguments(tokens[0] as LiteralToken)
        for (token in tokens.drop(1)) {
            arguments.addToken(token as ArgumentToken)
        }

        val commandObj = when (arguments.getCommandName()) {
            "parse" -> ParseCommand(arguments)
            "set" -> SetCommand(arguments)
            "keep" -> KeepCommand(arguments)
            else -> throw IndexOutOfBoundsException("Unknown command: ${arguments.getCommandName()}")
        }

        var processedMessage: Message? = null
        commandObj.output.addSubscriber { processedMessage = it }
        commandObj.input(message)

        return processedMessage
    }

    protected fun processMessageWithConfig(message: Message, config: String): Message? {
        val root = RootCommand.fromTokens(Token.getTokens(config, "test-config"))
        root.start()

        val commandObj = root.subcommands.first()

        var processedMessage: Message? = null
        commandObj.output.addSubscriber { processedMessage = it }
        commandObj.input(message)

         return processedMessage
     }

    protected fun getMessageDump(message: Message)
        = DumpFormatter().formatMessage(message).replace(Regex("^.*\n"), "")

    protected fun assertConfigParses(fromText: String, toDefinition: String) {
        assertEquals(toDefinition, Config(fromText, "test-config").getDefinition())
    }

    protected fun assertConfigFails(fromText: String, errorMessage: String) {
        try {
            val definition = Config(fromText, "test-config").getDefinition()
            fail("This config should not parse correctly: $fromText\n=== parsed ===\n$definition\n===")
        } catch (e: ParseException) {
            assertEquals(errorMessage, e.message)
        }
    }

    companion object {
        @JvmStatic @BeforeClass
        fun beforeAllTests() {
            InternalLog.stopWritingToStdout()
            InternalLog.stopWritingToStderr()
        }
    }
}
