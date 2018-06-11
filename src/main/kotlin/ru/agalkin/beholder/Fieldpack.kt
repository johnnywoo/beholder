package ru.agalkin.beholder

typealias Writer = (source: ByteArray, readLength: Int) -> Unit
typealias Reader = (length: Int) -> Fieldpack.Chunk

/**
 * Fieldpack:
 *
 * -- NUM F = number of field names
 * -- F x ( NSTR field name )
 * -- NUM M = number of messages
 * -- M x ( fields of a message = F x ( NSTR field value ) )
 *
 * NSTR = NUM L + L bytes string
 *
 * NUM is MSB varint encoding, least significant group first
 * -- set I = 0
 * -- read byte X (8 bit)
 *    value += (X & 0b01111111) << (7 * I)
 *    if (X & 0b10000000) != 0, then I++ and read next byte
 *
 * NUM examples:
 * value -> NUM
 * 0     -> 0x00      00000000
 * 1     -> 0x01      00000001
 * 127   -> 0x7F      01111111
 * 128   -> 0x8001    10000000 00000001
 * 129   -> 0x8101    10000001 00000001
 * 16383 -> 0xFF7F    11111111 01111111
 *
 * Most fields will use 1 byte for field name length.
 * Most non-payload fields will use 1 byte for value length.
 * Most payloads will use 2 bytes for payload length. Nginx access log syslog payload almost always has length from 100 to 500 bytes.
 */
class Fieldpack {
    open class Chunk(val data: ByteArray, val offset: Int = 0, private val length: Int = data.size - offset) {
        override fun toString()
            = String(data, offset, length, Charsets.UTF_8)

        open fun toFieldValue(): FieldValue {
            if (offset == 0) {
                return FieldValue.fromByteArray(data, offset)
            }
            val croppedData = data.copyOfRange(offset, offset + length)
            return FieldValue.fromByteArray(croppedData, length)
        }

        companion object {
            val empty = object : Chunk(ByteArray(0)) {
                override fun toString()
                    = ""

                override fun toFieldValue()
                    = FieldValue.empty
            }
        }
    }

    class FieldpackException(message: String) : Exception(message)

    fun readMessages(read: Reader): List<Message> {
        // NUM F = number of field names
        val fieldNumber = readNum(read)

        // F x ( NSTR field name )
        val fieldNames = mutableListOf<String>()
        for (i in 0 until fieldNumber) {
            val fieldNameChunk = readNumStr(read)
            fieldNames.add(fieldNameChunk.toString())
        }

        // NUM M = number of messages
        val messageNumber = readNum(read)

        // M x F x ( NSTR value )  fields of a message are next to each other
        val result = mutableListOf<Message>()
        for (i in 0 until messageNumber) {
            val message = Message()
            for (fieldName in fieldNames) {
                message.setFieldValue(fieldName, readNumStr(read).toFieldValue())
            }
            result.add(message)
        }

        return result
    }

    fun writeMessages(messages: List<Message>, write: Writer): Int {
        val fieldNames = mutableListOf<String>()
        for (message in messages) {
            for (fieldName in message.getFieldNames()) {
                if (!fieldNames.contains(fieldName)) {
                    fieldNames.add(fieldName)
                }
            }
        }
        return writeMessages(messages, fieldNames, write)
    }

    fun writeMessages(messages: List<Message>, fields: List<String>, write: Writer): Int {
        // sorting makes output fully reproducible and does not really cost much
        val fieldNames = fields.sorted()

        var packedLength = 0

        // NUM F = number of field names
        packedLength += writeNum(fieldNames.size, write)

        // F x ( NSTR field name )
        for (fieldName in fieldNames) {
            val ba = fieldName.toByteArray(Charsets.UTF_8)
            packedLength += writeNumStr(ba, ba.size, write)
        }

        // NUM M = number of messages
        packedLength += writeNum(messages.size, write)

        // M x F x ( NSTR value )
        for (message in messages) {
            for (fieldName in fieldNames) {
                val value = message.getFieldValue(fieldName)
                packedLength += writeNumStr(value.toByteArray(), value.getByteLength(), write)
            }
        }

        return packedLength
    }


    //
    // NUM strings
    //

    private inline fun writeNumStr(value: ByteArray, readLength: Int, write: Writer): Int {
        var writtenLength = 0

        writtenLength += writeNum(readLength, write)

        write(value, readLength)
        writtenLength += readLength

        return writtenLength
    }

    private inline fun readNumStr(read: Reader): Chunk {
        val length = readNum(read)
        if (length == 0L) {
            return Chunk.empty
        }
        return read(length.toInt())
    }



    //
    // NUM sequences
    //

    /**
     * Converts N to NUM-sequence of bytes, returns length of the sequence
     */
    inline fun writeNum(n: Number, write: Writer): Int {
        val numBuffer = ByteArray(10)
        var remainder = n.toLong()
        var size = 1
        while (size < 10) {
            val carryBit = if (remainder > 0b0111_1111) 0b1000_0000 else 0
            numBuffer[size - 1] = (carryBit or (remainder.toInt() and 0b0111_1111)).toByte()
            if (carryBit == 0) {
                write(numBuffer, size)
                return size
            }
            remainder = remainder shr 7
            size++
        }
        throw FieldpackException("Invalid NUM sequence: number is too big")
    }

    inline fun readNum(read: Reader): Long {
        var n = 0L
        var size = 0
        while (size < 10) {
            val chunk = read(1)
            val byteVal = chunk.data[chunk.offset].toInt() and 0xff

            n += (byteVal and 0b0111_1111) shl (7 * size)
            size++

            if ((byteVal and 0b1000_0000) == 0) {
                return n
            }
        }
        throw FieldpackException("Invalid NUM sequence: too many bytes")
    }
}
