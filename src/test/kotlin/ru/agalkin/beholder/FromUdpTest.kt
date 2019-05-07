package ru.agalkin.beholder

import ru.agalkin.beholder.testutils.NetworkedTestAbstract
import ru.agalkin.beholder.testutils.assertByteArraysEqual
import ru.agalkin.beholder.testutils.assertFieldNames
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FromUdpTest : NetworkedTestAbstract() {
    @Test
    fun testFromUdpSimple() {
        val messageText = "message"
        val processedMessage = feedMessagesIntoConfig("from udp 3820") {
            sendToUdp(3820, messageText)
        }

        assertNotNull(processedMessage)
        assertFieldNames(processedMessage, "date", "from", "payload")
        assertEquals(messageText, processedMessage.getPayloadString())
    }

    @Test
    fun testFromUdpTwoMessages() {
        val processedMessages = feedMessagesIntoConfig("from udp 3820", 2) {
            sendToUdp(3820, "cat")
            sendToUdp(3820, "dog")
        }

        assertEquals(2, processedMessages.size)
        assertEquals("cat, dog", processedMessages.joinToString(", ") { it.getPayloadString() })
    }

    @Test
    fun testFromUdpMultiline() {
        val messageText = "first\nsecond"
        val processedMessage = feedMessagesIntoConfig("from udp 3820") {
            sendToUdp(3820, messageText)
        }

        assertNotNull(processedMessage)
        assertFieldNames(processedMessage, "date", "from", "payload")
        assertEquals(messageText, processedMessage.getPayloadString())
    }

    @Test
    fun testFromUdpZeroByte() {
        val messageBytes = byteArrayOf('a'.toByte(), 0.toByte(), 'b'.toByte())
        val processedMessage = feedMessagesIntoConfig("from udp 3820") {
            sendToUdp(3820, messageBytes)
        }

        assertNotNull(processedMessage)
        assertFieldNames(processedMessage, "date", "from", "payload")
        assertByteArraysEqual(messageBytes, messageBytes.slice(0 until messageBytes.size).toByteArray())
        assertByteArraysEqual(messageBytes, getByteArrayField(processedMessage, "payload"))
    }

    @Test
    fun testFromInvalidUnicodeBytes() {
        val messageBytes = byteArrayOf('a'.toByte(), 195.toByte(), 40.toByte(), 'b'.toByte())
        val processedMessage = feedMessagesIntoConfig("from udp 3820") {
            sendToUdp(3820, messageBytes)
        }

        assertNotNull(processedMessage)
        assertFieldNames(processedMessage, "date", "from", "payload")
        assertByteArraysEqual(messageBytes, messageBytes.slice(0 until messageBytes.size).toByteArray())
        assertByteArraysEqual(messageBytes, getByteArrayField(processedMessage, "payload"))
    }

    @Test
    fun testFromCyrillicSymbols() {
        val messageBytes = "кошка".toByteArray()
        val processedMessage = feedMessagesIntoConfig("from udp 3820") {
            sendToUdp(3820, messageBytes)
        }

        assertNotNull(processedMessage)
        assertFieldNames(processedMessage, "date", "from", "payload")
        assertEquals("кошка", processedMessage.getPayloadString())
        assertByteArraysEqual(messageBytes, messageBytes.slice(0 until messageBytes.size).toByteArray())
        assertByteArraysEqual(messageBytes, getByteArrayField(processedMessage, "payload"))
    }

    @Test
    fun testStringOperations() {
        val messageText = "cat"
        val processedMessage = feedMessagesIntoConfig("from udp 3820; set \$payload '\$payload-dog'") {
            sendToUdp(3820, messageText)
        }

        assertNotNull(processedMessage)
        assertEquals("cat-dog", processedMessage.getPayloadString())
    }

    @Test
    fun testZeroByteStringOperations() {
        val messageBytes = byteArrayOf('a'.toByte(), 0.toByte(), 'b'.toByte())
        val processedMessage = feedMessagesIntoConfig("from udp 3820; set \$payload '\$payload-dog'") {
            sendToUdp(3820, messageBytes)
        }

        assertNotNull(processedMessage)
        assertEquals("a\u0000b-dog", processedMessage.getPayloadString())
    }

    @Test
    fun testInvalidUnicodeBytesStringOperations() {
        val messageBytes = byteArrayOf('a'.toByte(), 195.toByte(), 40.toByte(), 'b'.toByte())
        val processedMessage = feedMessagesIntoConfig("from udp 3820; set \$payload '\$payload-dog'") {
            sendToUdp(3820, messageBytes)
        }

        assertNotNull(processedMessage)
        assertEquals("a�(b-dog", processedMessage.getPayloadString())
    }
}
