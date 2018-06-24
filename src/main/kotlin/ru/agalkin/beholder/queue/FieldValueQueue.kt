package ru.agalkin.beholder.queue

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Fieldpack
import ru.agalkin.beholder.config.ConfigOption

class FieldValueQueue(app: Beholder, receive: (FieldValue) -> Received) : BeholderQueueAbstract<FieldValue>(app, receive) {
    override fun createChunk(): Chunk<FieldValue> {
        return FieldValueChunk(app.getIntOption(ConfigOption.QUEUE_CHUNK_MESSAGES), buffer)
    }

    class FieldValueChunk(capacity: Int, buffer: DataBuffer) : Chunk<FieldValue>(capacity, buffer) {
        override fun pack(list: List<FieldValue>): ByteArray {
            var length = 0
            for (item in list) {
                val byteLength = item.getByteLength()
                length += fieldpack.writeNum(byteLength, { _, _ -> }) + byteLength
            }

            val bytes = ByteArray(length)
            var offset = 0
            for (item in list) {
                val byteLength = item.getByteLength()
                fieldpack.writeNum(byteLength, { source, readLength ->
                    for (i in 0 until readLength) {
                        bytes[offset + i] = source[i]
                    }
                    offset += readLength
                })
                val ba = item.toByteArray()
                for (i in 0 until byteLength) {
                    bytes[offset + i] = ba[i]
                }
                offset += byteLength
            }
            return bytes
        }

        override fun unpack(bytes: ByteArray): MutableList<FieldValue> {
            val list = mutableListOf<FieldValue>()

            var offset = 0
            while (offset < bytes.size) {
                val byteLength = (fieldpack.readNum { length ->
                    val portion = Fieldpack.Portion(bytes, offset, length)
                    offset += length
                    portion
                }).toInt()
                list.add(FieldValue.fromByteArray(
                    bytes.copyOfRange(offset, byteLength),
                    byteLength
                ))
                offset += byteLength
            }
            return list
        }
    }

    companion object {
        private val fieldpack = Fieldpack()
    }
}
