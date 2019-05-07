package ru.agalkin.beholder

import ru.agalkin.beholder.testutils.NetworkedTestAbstract
import ru.agalkin.beholder.testutils.assertFieldNames
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FromTcpTest : NetworkedTestAbstract() {
    @Test
    fun testFromTcpSimple() {
        val messageText = "message"
        val processedMessage = feedMessagesIntoConfig("from tcp 3820") {
            sendToTcp(3820, (messageText + "\n").toByteArray())
        }

        assertNotNull(processedMessage)
        assertFieldNames(processedMessage, "date", "from", "payload")
        assertEquals(messageText, processedMessage.getPayloadString())
    }

    @Test
    fun testFromTcpTwoMessages() {
        val processedMessages = feedMessagesIntoConfig("from tcp 3820", 2) {
            sendToTcp(3820, "cat\ndog\n".toByteArray())
        }

        assertEquals(2, processedMessages.size)
        assertEquals("cat", processedMessages[0].getPayloadString())
        assertEquals("dog", processedMessages[1].getPayloadString())
    }
}
