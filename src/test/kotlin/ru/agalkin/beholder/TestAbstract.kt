package ru.agalkin.beholder

import org.junit.BeforeClass
import ru.agalkin.beholder.commands.KeepCommand
import ru.agalkin.beholder.commands.ParseCommand
import ru.agalkin.beholder.commands.SetCommand
import ru.agalkin.beholder.config.Config
import ru.agalkin.beholder.config.expressions.CommandAbstract
import ru.agalkin.beholder.config.expressions.CommandArguments
import ru.agalkin.beholder.config.expressions.RootCommand
import ru.agalkin.beholder.config.parser.ArgumentToken
import ru.agalkin.beholder.config.parser.LiteralToken
import ru.agalkin.beholder.config.parser.ParseException
import ru.agalkin.beholder.config.parser.Token
import ru.agalkin.beholder.formatters.DumpFormatter
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
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

        var processedMessage: Message? = null
        root.subcommands.last().output.addSubscriber { processedMessage = it }
        root.subcommands[0].input(message)

        return processedMessage
    }

    protected fun receiveMessageWithConfig(config: String, senderBlock: (CommandAbstract) -> Unit): Message? {
        return receiveMessagesWithConfig(config, 1, senderBlock).firstOrNull()
    }

    protected fun receiveMessagesWithConfig(config: String, count: Int, senderBlock: (CommandAbstract) -> Unit): List<Message> {
        val root = RootCommand.fromTokens(Token.getTokens(config, "test-config"))

        val processedMessages = mutableListOf<Message>()
        root.subcommands.last().output.addSubscriber { processedMessages.add(it) }

        root.start()

        senderBlock(root.subcommands.first())

        var timeSpentMillis = 0
        while (timeSpentMillis < 300) {
            if (processedMessages.size == count) {
                break
            }
            Thread.sleep(50)
            timeSpentMillis += 50
        }

        root.stop()
        assertEquals(count, processedMessages.size, "Expected number of messages does not match")

        return processedMessages
    }

    protected fun getMessageDump(message: Message?)
        = when (message) {
            null -> "null"
            else -> DumpFormatter().formatMessage(message).toString().replace(Regex("^.*\n"), "")
        }

    protected fun assertFieldNames(message: Message?, vararg names: String) {
        assertNotNull(message)
        if (message != null) {
            assertEquals(names.sorted(), message.getFieldNames().sorted())
        }
    }

    protected fun assertFieldValues(message: Message?, values: Map<String, String>) {
        assertNotNull(message)
        if (message != null) {
            assertEquals(message.getFieldNames().sorted(), values.keys.sorted())
            for ((key, value) in values) {
                assertEquals(value, message.getStringField(key))
            }
        }
    }

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

    protected fun getByteArrayField(message: Message, field: String): ByteArray {
        val fieldValue = message.getFieldValue(field)
        return fieldValue.toByteArray().slice(0 until fieldValue.getByteLength()).toByteArray()
    }

    protected fun assertByteArraysEqual(a: ByteArray, b: ByteArray) {
        if (a.size != b.size) {
            assertTrue(false, "Byte arrays differ in size: ${a.size}, ${b.size}")
        }
        for (i in a.indices) {
            if (a[i] != b[i]) {
                assertTrue(false, "Different bytes at position $i: '${a[i].toInt()}', '${b[i].toInt()}'")
            }
        }
    }

    companion object {
        @JvmStatic @BeforeClass
        fun beforeAllTests() {
            InternalLog.setStdout(null)
            InternalLog.setStderr(null)
        }
    }
}
