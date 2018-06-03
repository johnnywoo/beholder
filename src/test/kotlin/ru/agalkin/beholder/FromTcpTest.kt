package ru.agalkin.beholder

import org.junit.Test
import java.net.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FromTcpTest : TestAbstract() {
    @Test
    fun testFromTcpSimple() {
        SelectorThread.erase()

        val messageText = "message"
        val processedMessage = receiveMessageWithConfig("from tcp 3820") {
            val port = 3820
            Socket().use { socket ->
                socket.connect(InetSocketAddress(InetAddress.getLocalHost(), port))
                socket.getOutputStream().write((messageText + "\n").toByteArray())
            }
        }

        assertNotNull(processedMessage)
        if (processedMessage == null) {
            return
        }
        assertFieldNames(processedMessage, "date", "from", "payload")
        assertEquals(messageText, processedMessage.getPayloadString())
    }

    @Test
    fun testFromTcpTwoMessages() {
        SelectorThread.erase()

        val processedMessages = receiveMessagesWithConfig("from tcp 3820", 2) {
            val port = 3820
            Socket().use { socket ->
                socket.connect(InetSocketAddress(InetAddress.getLocalHost(), port))
                socket.getOutputStream().write("cat\ndog\n".toByteArray())
            }
        }

        assertEquals(2, processedMessages.size)
        assertEquals("cat", processedMessages[0].getPayloadString())
        assertEquals("dog", processedMessages[1].getPayloadString())
    }
}
