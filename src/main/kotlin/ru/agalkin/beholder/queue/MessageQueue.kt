package ru.agalkin.beholder.queue

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.Fieldpack
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.ConfigOption
import ru.agalkin.beholder.threadLocal

class MessageQueue(app: Beholder, receive: (Message) -> Received) : BeholderQueueAbstract<Message>(app, receive) {
    override fun createChunk(): Chunk<Message> {
        return MessageChunk(app.getIntOption(ConfigOption.QUEUE_CHUNK_MESSAGES), buffer)
    }

    class MessageChunk(capacity: Int, buffer: DataBuffer) : Chunk<Message>(capacity, buffer) {
        override fun pack(list: List<Message>): ByteArray {
            val bytes = ByteArray(fieldpack.getPackedLength(list))

            var offset = 0
            fieldpack.writeMessages(list) { source, length ->
                for (i in 0 until length) {
                    bytes[offset + i] = source[i]
                }
                offset += length
            }
            return bytes
        }

        override fun unpack(bytes: ByteArray): MutableList<Message> {
            val list = mutableListOf<Message>()

            var offset = 0
            fieldpack.readMessagesTo(list) { length ->
                val stringValue = Fieldpack.Portion(bytes, offset, length)
                offset += length
                stringValue
            }
            return list
        }
    }

    companion object {
        private val fieldpack by threadLocal { Fieldpack() }
    }
}
