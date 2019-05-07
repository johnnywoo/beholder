package ru.agalkin.beholder.testutils

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.expressions.CommandAbstract
import ru.agalkin.beholder.config.expressions.RootCommand
import java.net.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

abstract class NetworkedTestAbstract : TestAbstract() {
    protected fun feedMessagesIntoConfig(config: String, senderBlock: (CommandAbstract) -> Unit): Message? {
        return feedMessagesIntoConfig(config, 1, senderBlock).firstOrNull()
    }

    protected fun makeAppAndMock(config: String, testBlock: (Beholder, Mock) -> Unit) {
        makeApp(config).use { app ->
            app.config.start()
            val mock = Mock(app)
            testBlock(app, mock)
            mock.finalize()
        }
    }

    class Mock(private val app: Beholder) {
        fun send(vararg pairs: Pair<String, String>) {
            send(Message.of(*pairs))
        }

        private fun send(message: Message) {
            app.mockListeners["default"]!!.queue.add(message)
        }

        private val receivedNum = AtomicInteger(0)

        fun receive(): FieldValue {
            val received = app.mockSenders["default"]!!.received
            for (i in 1..25) {
                Thread.sleep(2)
                if (!(received.size < receivedNum.get() + 1)) {
                    break
                }
            }
            if (received.size < receivedNum.get() + 1) {
                throw Exception("A message is expected at `from mock`, but there is no message: expected ${receivedNum.get() + 1}, actual ${received.size}")
            }
            val fieldValue = received[receivedNum.get()]
            receivedNum.incrementAndGet()
            return fieldValue
        }

        fun finalize() {
            Thread.sleep(50)
            val output = app.mockSenders["default"]
            if (output != null && output.received.size != receivedNum.get()) {
                throw Exception("Mock received too many messages: expected $receivedNum, actual ${output.received.size}")
            }
        }
    }

    protected fun feedMessagesIntoConfig(config: String, count: Int, senderBlock: (RootCommand) -> Unit): List<Message> {
        val processedMessages = mutableListOf<Message>()

        makeApp(config).use { app ->
            val root = app.config.root

            root.topLevelOutput.addStep(conveyorStepOf {
                processedMessages.add(it)
            })

            root.start()
            Thread.sleep(100)

            senderBlock(root)

            Thread.sleep(200)

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
}
