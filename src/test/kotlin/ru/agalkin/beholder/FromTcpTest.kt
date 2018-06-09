package ru.agalkin.beholder

import org.junit.Test
import ru.agalkin.beholder.listeners.SelectorThread
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FromTcpTest : TestAbstract() {
    @Test
    fun testFromTcpSimple() {
        SelectorThread.erase()

        val messageText = "message"
        val processedMessage = receiveMessageWithConfig("from tcp 3820") {
            sendToTcp(3820, (messageText + "\n").toByteArray())
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
            sendToTcp(3820, "cat\ndog\n".toByteArray())
        }

        assertEquals(2, processedMessages.size)
        assertEquals("cat", processedMessages[0].getPayloadString())
        assertEquals("dog", processedMessages[1].getPayloadString())
    }
}
