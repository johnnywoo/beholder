package ru.agalkin.beholder

object Fieldpack {
    private const val MAX_LITERAL_NUM = 249L

    /**
     * Converts N to NUM-sequence of bytes, returns length of the sequence
     */
    fun writeNum(n: Long, buffer: ByteArray? = null, offset: Int = 0): Int {
        var tailLength = 0
        var remainder = n - MAX_LITERAL_NUM
        while (remainder > 0) {
            remainder--
            tailLength++

            if (buffer != null) {
                val unsignedByte = remainder and 0xff
                buffer[offset + tailLength] = unsignedByte.toByte()
            }

            remainder = remainder shr 8
            if (tailLength > 6) {
                throw BeholderException("Too big for NUM sequence: $n")
            }
        }

        if (buffer != null) {
            if (tailLength == 0) {
                buffer[offset] = n.toByte()
            } else {
                buffer[offset] = (MAX_LITERAL_NUM + tailLength).toByte()
            }
        }

        return 1 + tailLength
    }

    fun readNum(byteMaker: () -> Byte): Long {
        val unsignedFirstByte = byteMaker().toLong() and 0xff
        if (unsignedFirstByte <= MAX_LITERAL_NUM) {
            return unsignedFirstByte
        }
        var n = MAX_LITERAL_NUM
        for (i in 0 until unsignedFirstByte - MAX_LITERAL_NUM) {
            val unsignedByte = byteMaker().toLong() and 0xff
            n += (unsignedByte + 1) shl (8 * i).toInt()
        }
        return n
    }
}
