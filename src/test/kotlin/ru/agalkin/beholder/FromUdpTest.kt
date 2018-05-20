package ru.agalkin.beholder

import org.junit.Test
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FromUdpTest : TestAbstract() {
    @Test
    fun testFromUdpSimple() {
        val messageText = "message"
        val processedMessage = receiveMessageWithConfig("from udp 3820") {
            sendToUdp(3820, messageText)
        }

        assertNotNull(processedMessage)
        if (processedMessage == null) {
            return
        }
        assertFieldNames(processedMessage, "date", "from", "payload")
        assertEquals(messageText, processedMessage.getPayloadString())
    }

    @Test
    fun testFromUdpTwoMessages() {
        val processedMessages = receiveMessagesWithConfig("from udp 3820", 2) {
            sendToUdp(3820, "cat")
            sendToUdp(3820, "dog")
        }

        assertEquals(2, processedMessages.size)
        assertEquals("cat, dog", processedMessages.joinToString(", ") { it.getPayloadString() })
    }

    @Test
    fun testFromUdpMultiline() {
        val messageText = "first\nsecond"
        val processedMessage = receiveMessageWithConfig("from udp 3820") {
            sendToUdp(3820, messageText)
        }

        assertNotNull(processedMessage)
        if (processedMessage == null) {
            return
        }
        assertFieldNames(processedMessage, "date", "from", "payload")
        assertEquals(messageText, processedMessage.getPayloadString())
    }

    @Test
    fun testFromUdpZeroByte() {
        val messageBytes = byteArrayOf('a'.toByte(), 0.toByte(), 'b'.toByte())
        val processedMessage = receiveMessageWithConfig("from udp 3820") {
            sendToUdp(3820, messageBytes)
        }

        assertNotNull(processedMessage)
        if (processedMessage == null) {
            return
        }
        assertFieldNames(processedMessage, "date", "from", "payload")
        assertByteArraysEqual(messageBytes, messageBytes.slice(0 until messageBytes.size).toByteArray())
        assertByteArraysEqual(messageBytes, getByteArrayField(processedMessage, "payload"))
    }

    @Test
    fun testFromInvalidUnicodeBytes() {
        val messageBytes = byteArrayOf('a'.toByte(), 195.toByte(), 40.toByte(), 'b'.toByte())
        val processedMessage = receiveMessageWithConfig("from udp 3820") {
            sendToUdp(3820, messageBytes)
        }

        assertNotNull(processedMessage)
        if (processedMessage == null) {
            return
        }
        assertFieldNames(processedMessage, "date", "from", "payload")
        assertByteArraysEqual(messageBytes, messageBytes.slice(0 until messageBytes.size).toByteArray())
        assertByteArraysEqual(messageBytes, getByteArrayField(processedMessage, "payload"))
    }

    @Test
    fun testStringOperations() {
        val messageText = "cat"
        val processedMessage = receiveMessageWithConfig("from udp 3820; set \$payload '\$payload-dog'") {
            sendToUdp(3820, messageText)
        }

        assertNotNull(processedMessage)
        if (processedMessage == null) {
            return
        }
        assertEquals("cat-dog", processedMessage.getPayloadString())
    }

    @Test
    fun testZeroByteStringOperations() {
        val messageBytes = byteArrayOf('a'.toByte(), 0.toByte(), 'b'.toByte())
        val processedMessage = receiveMessageWithConfig("from udp 3820; set \$payload '\$payload-dog'") {
            sendToUdp(3820, messageBytes)
        }

        assertNotNull(processedMessage)
        if (processedMessage == null) {
            return
        }
        assertEquals("a\u0000b-dog", processedMessage.getPayloadString())
    }

    @Test
    fun testInvalidUnicodeBytesStringOperations() {
        val messageBytes = byteArrayOf('a'.toByte(), 195.toByte(), 40.toByte(), 'b'.toByte())
        val processedMessage = receiveMessageWithConfig("from udp 3820; set \$payload '\$payload-dog'") {
            sendToUdp(3820, messageBytes)
        }

        assertNotNull(processedMessage)
        if (processedMessage == null) {
            return
        }
        assertEquals("a�(b-dog", processedMessage.getPayloadString())
    }

    private fun sendToUdp(port: Int, message: String) {
        sendToUdp(port, message.toByteArray())
    }

    private fun sendToUdp(port: Int, message: ByteArray) {
        DatagramSocket().send(
            DatagramPacket(message, message.size, InetAddress.getLocalHost(), port)
        )
    }
}
