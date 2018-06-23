package ru.agalkin.beholder

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class FieldpackTest : TestAbstract() {
    private val fieldpack = Fieldpack()

    @Test
    fun testFieldpackUnpackTrivial() {
        val message = Message(mapOf(
            "payload" to FieldValue.fromString("cat")
        ))

        val buffer = ByteArray(10000)
        var length = 0

        // packing and writing
        fieldpack.writeMessages(listOf(message)) { source, readLength ->
            for (i in 0 until readLength) {
                buffer[length++] = source[i]
            }
        }

        assertByteArraysEqual(byteArrayOf(
            // * NUM N = number of field names
            1,
            // * N x ( NSTR field name )
            7, *bytes("payload"),
            // * NUM M = number of messages
            1,
            // * M x N x ( NSTR value )
            3, *bytes("cat")
        ), buffer.copyOfRange(0, length))

        // unpacking
        var index = 0
        val unpackedMessages = fieldpack.readMessages { readLength ->
            val portion = Fieldpack.Portion(buffer, index, readLength)
            index += readLength
            if (index > length) {
                fail("readMessages tried to read $index bytes, packed length $length")
            }
            portion
        }

        assertEquals(1, unpackedMessages.size)
        assertFieldNames(unpackedMessages[0], "payload")
        assertEquals("cat", unpackedMessages[0].getPayloadString())
    }

    private fun bytes(s: String)
        = s.toByteArray()

    @Test
    fun testFieldpackUnpackTwoMessages() {
        val messages = listOf(
            Message(mapOf(
                "sound" to FieldValue.fromString("meow"),
                "claws" to FieldValue.fromString("retracted")
            )),
            Message(mapOf(
                "sound" to FieldValue.fromString("gruff"),
                "tail" to FieldValue.fromString("wiggling")
            ))
        )

        val buffer = ByteArray(10000)
        var length = 0

        // packing and writing
        fieldpack.writeMessages(messages) { source, readLength ->
            for (i in 0 until readLength) {
                buffer[length++] = source[i]
            }
        }

        assertByteArraysEqual(byteArrayOf(
            // * NUM N = number of field names
            3,
            // * N x ( NSTR field name )
            5, *bytes("claws"),
            5, *bytes("sound"),
            4, *bytes("tail"),
            // * NUM M = number of messages
            2,
            // * M x N x ( NSTR value )
            9, *bytes("retracted"),
            4, *bytes("meow"),
            0, // no "tail" in first message
            0, // no "claws" in second message
            5, *bytes("gruff"),
            8, *bytes("wiggling")
        ), buffer.copyOfRange(0, length))

        // unpacking
        var index = 0
        val unpackedMessages = fieldpack.readMessages { readLength ->
            val portion = Fieldpack.Portion(buffer, index, readLength)
            index += readLength
            if (index > length) {
                fail("readMessages tried to read $index bytes, packed length $length")
            }
            portion
        }

        assertEquals(2, unpackedMessages.size)

        assertFieldNames(unpackedMessages[0], "claws", "sound")
        assertEquals("meow", unpackedMessages[0].getStringField("sound"))
        assertEquals("retracted", unpackedMessages[0].getStringField("claws"))

        assertFieldNames(unpackedMessages[1], "tail", "sound")
        assertEquals("gruff", unpackedMessages[1].getStringField("sound"))
        assertEquals("wiggling", unpackedMessages[1].getStringField("tail"))
    }

    @Test
    fun testNumPackUnpack() {
        // some small numbers
        for (n in 0..33000L) {
            val byteArray = ByteArray(10)
            val length = fieldpack.writeNum(n) { source, readLength ->
                for (i in 0 until readLength) {
                    byteArray[i] = source[i]
                }
            }
            var i = 0
            val unpackedN = fieldpack.readNum { toRead ->
                val portion = Fieldpack.Portion(byteArray, i, toRead)

                i += toRead
                if (i > length) {
                    throw BeholderException("Too many reads: $i original=$n packed=" + byteArray.joinToString(",") { (it.toInt() and 0xff).toString() })
                }
                portion
            }
            assertEquals(n, unpackedN, "Invalid unpack: unpacked=$unpackedN original=$n packed=" + byteArray.joinToString(",") { (it.toInt() and 0xff).toString() })
        }
    }

    @Test
    fun testNumPackBytes() {
        assertBytes(uByteArray(0b0000_0000), 0)
        assertBytes(uByteArray(0b0000_0001), 1)
        assertBytes(uByteArray(0b0111_1111), 127)
        assertBytes(uByteArray(0b1000_0000, 0b0000_0001), 128)
        assertBytes(uByteArray(0b1000_0001, 0b0000_0001), 129)
        assertBytes(uByteArray(0b1111_1111, 0b0000_0001), 255)
        assertBytes(uByteArray(0b1000_0000, 0b0000_0010), 256)
        assertBytes(uByteArray(0b1010_1100, 0b0000_0010), 300)
        assertBytes(uByteArray(0b1111_1111, 0b0111_1111), 16383)
        assertBytes(uByteArray(0b1000_0000, 0b1000_0000, 0b0000_0001), 16384)
    }

    private fun assertBytes(expected: ByteArray, n: Long) {
        val actual = createNumSequence(n)
        assertEquals(
            expected.joinToString { (it.toInt() and 0xff).toString() },
            actual.joinToString { (it.toInt() and 0xff).toString() },
            "Bytes do not match: n=$n expected=${expected.joinToString(",") { (it.toInt() and 0xff).toString() }} actual=${actual.joinToString(",") { (it.toInt() and 0xff).toString() }}"
        )
    }

    private fun uByteArray(vararg bytes: Short): ByteArray {
        val byteArray = ByteArray(bytes.size)
        var i = 0
        for (ubyte in bytes) {
            byteArray[i++] = ubyte.toByte()
        }
        return byteArray
    }

    private fun createNumSequence(n: Long): ByteArray {
        val length = fieldpack.writeNum(n, {_,_->})
        val byteArray = ByteArray(length)
        fieldpack.writeNum(n) { source, readLenth ->
            for (i in 0 until readLenth) {
                byteArray[i] = source[i]
            }
        }
        return byteArray
    }
}
