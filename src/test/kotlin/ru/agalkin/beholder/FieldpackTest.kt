package ru.agalkin.beholder

import org.junit.Test
import kotlin.test.assertEquals

class FieldpackTest : TestAbstract() {
    @Test
    fun testNumPackUnpack() {
        // some small numbers
        for (n in 0..10000L) {
            val byteArray = ByteArray(10)
            val length = Fieldpack.writeNum(n, byteArray)
            var i = 0
            val unpackedN = Fieldpack.readNum {
                if (i >= length) {
                    throw BeholderException("Too many reads: $i original=$n packed=" + byteArray.joinToString(",") { (it.toInt() and 0xff).toString() })
                }
                byteArray[i++]
            }
            assertEquals(n, unpackedN, "Invalid unpack: unpacked=$unpackedN original=$n packed=" + byteArray.joinToString(",") { (it.toInt() and 0xff).toString() })
        }
    }

    @Test
    fun testNumPackBytes() {
        assertBytes(uByteArray(0), 0)
        assertBytes(uByteArray(1), 1)
        assertBytes(uByteArray(248), 248)
        assertBytes(uByteArray(249), 249)

        assertBytes(uByteArray(250,   0), 250)
        assertBytes(uByteArray(250,   1), 251)
        assertBytes(uByteArray(250, 254), 504)
        assertBytes(uByteArray(250, 255), 505)

        assertBytes(uByteArray(251,   0,   0), 506)
        assertBytes(uByteArray(251,   1,   0), 507)
        assertBytes(uByteArray(251, 254, 255), 66_040)
        assertBytes(uByteArray(251, 255, 255), 66_041)

        assertBytes(uByteArray(252,   0,   0,   0), 66_042)
        assertBytes(uByteArray(252,   1,   0,   0), 66_043)
        assertBytes(uByteArray(252, 254, 255, 255), 16_843_256)
        assertBytes(uByteArray(252, 255, 255, 255), 16_843_257)
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
        val length = Fieldpack.writeNum(n)
        val byteArray = ByteArray(length)
        Fieldpack.writeNum(n, byteArray)
        return byteArray
    }
}
