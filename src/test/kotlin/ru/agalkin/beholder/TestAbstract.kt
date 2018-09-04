package ru.agalkin.beholder

import org.junit.BeforeClass
import ru.agalkin.beholder.config.Config
import ru.agalkin.beholder.config.expressions.CommandAbstract
import ru.agalkin.beholder.config.expressions.RootCommand
import ru.agalkin.beholder.config.parser.ParseException
import ru.agalkin.beholder.formatters.DumpFormatter
import java.net.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

abstract class TestAbstract {
    protected fun processMessageWithConfig(message: Message, config: String): Message? {
        var processedMessage: Message? = null

        makeApp(config).use { app ->
            val root = app.config.root
            root.start()

            root.topLevelOutput.addStep {
                processedMessage = it
                return@addStep Conveyor.StepResult.CONTINUE
            }

            root.topLevelInput.addMessage(message)
        }

        return processedMessage
    }

    protected fun receiveMessageWithConfig(config: String, senderBlock: (CommandAbstract) -> Unit): Message? {
        return receiveMessagesWithConfig(config, 1, senderBlock).firstOrNull()
    }

    protected fun makeApp(config: String)
        = Beholder({ Config.fromStringWithLog(it, config, "test-config") })

    protected fun receiveMessagesWithConfig(config: String, count: Int, senderBlock: (RootCommand) -> Unit): List<Message> {
        val processedMessages = mutableListOf<Message>()

        makeApp(config).use { app ->
            val root = app.config.root

            root.topLevelOutput.addStep {
                processedMessages.add(it)
                return@addStep Conveyor.StepResult.CONTINUE
            }

            root.start()
            Thread.sleep(100)

            senderBlock(root)

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
        }

        return processedMessages
    }

    protected fun sendToUdp(port: Int, message: String) {
        sendToUdp(port, message.toByteArray())
    }

    protected fun sendToUdp(port: Int, message: ByteArray) {
        DatagramSocket().send(
            DatagramPacket(message, message.size, InetAddress.getLocalHost(), port)
        )
    }

    protected fun sendToTcp(port: Int, messageText: String) = sendToTcp(port, messageText.toByteArray())

    protected fun sendToTcp(port: Int, messageBytes: ByteArray) {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(InetAddress.getLocalHost(), port))
            socket.getOutputStream().write(messageBytes)
        }
    }

    protected fun getMessageDump(message: Message?) = when (message) {
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
        makeApp("").use { app ->
            assertEquals(toDefinition, Config(app, fromText, "test-config").getDefinition())
        }
    }

    protected fun assertConfigFails(fromText: String, errorMessage: String) {
        try {
            makeApp("").use { app ->
                val definition = Config(app, fromText, "test-config").getDefinition()
                fail("This config should not parse correctly: $fromText\n=== parsed ===\n$definition\n===")
            }
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
