package ru.agalkin.beholder

import org.junit.Test
import java.net.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FromTcpTest : TestAbstract() {
    @Test
    fun testFromTcpSimple() {
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
        assertEquals("date,from,payload", processedMessage.getFieldNames().sorted().joinToString(",") { it })
        assertEquals(messageText, processedMessage.getPayload())
    }

    @Test
    fun testFromTcpMultiple() {
        val processedMessages = receiveMessagesWithConfig("from tcp 3820", 2) {
            val port = 3820
            Socket().use { socket ->
                socket.connect(InetSocketAddress(InetAddress.getLocalHost(), port))
                socket.getOutputStream().write("cat\ndog\n".toByteArray())
            }
        }

        assertEquals(2, processedMessages.size)
        assertEquals("cat", processedMessages[0].getPayload())
        assertEquals("dog", processedMessages[1].getPayload())
    }
}
