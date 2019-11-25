package ru.agalkin.beholder.queue

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Fieldpack

class FieldValueQueue(app: Beholder, receive: (FieldValue) -> Received) : BeholderQueueAbstract<FieldValue>(app, receive) {
    override fun pack(list: List<FieldValue>): ByteArray {
        var length = 0
        for (item in list) {
            val byteLength = item.getByteLength()
            length += Fieldpack.writeNum(byteLength, { _, _ -> }) + byteLength
        }

        val bytes = ByteArray(length)
        var offset = 0
        for (item in list) {
            val byteLength = item.getByteLength()
            Fieldpack.writeNum(byteLength, { source, readLength ->
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

    override fun unpack(bytes: ByteArray): List<FieldValue> {
        val list = mutableListOf<FieldValue>()

        var offset = 0
        while (offset < bytes.size) {
            val byteLength = (Fieldpack.readNum { length ->
                val portion = Fieldpack.Portion(bytes, offset, length)
                offset += length
                portion
            }).toInt()
            list.add(FieldValue.fromByteArray(
                bytes.copyOfRange(offset, offset + byteLength),
                byteLength
            ))
            offset += byteLength
        }
        return list
    }
}
