package ru.agalkin.beholder

import ru.agalkin.beholder.testutils.TestAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FromTcpTest : TestAbstract() {
    @Test
    fun testFromTcpSimple() {
        val messageText = "message"
        val processedMessage = receiveMessageWithConfig("from tcp 3820") {
            sendToTcp(3820, (messageText + "\n").toByteArray())
        }

        assertNotNull(processedMessage)
        assertFieldNames(processedMessage, "date", "from", "payload")
        assertEquals(messageText, processedMessage.getPayloadString())
    }

    @Test
    fun testFromTcpTwoMessages() {
        val processedMessages = receiveMessagesWithConfig("from tcp 3820", 2) {
            sendToTcp(3820, "cat\ndog\n".toByteArray())
        }

        assertEquals(2, processedMessages.size)
        assertEquals("cat", processedMessages[0].getPayloadString())
        assertEquals("dog", processedMessages[1].getPayloadString())
    }
}
