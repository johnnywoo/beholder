package ru.agalkin.beholder.queue

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.Fieldpack
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.ConfigOption

class MessageQueue(app: Beholder, receive: (Message) -> Received) : BeholderQueueAbstract<Message>(app, receive) {
    override fun createChunk(): Chunk<Message> {
        return MessageChunk(app.getIntOption(ConfigOption.QUEUE_CHUNK_MESSAGES), buffer)
    }

    class MessageChunk(capacity: Int, buffer: DataBuffer) : Chunk<Message>(capacity, buffer) {
        override fun pack(list: List<Message>)
            = Fieldpack.writeMessagesToByteArray(list)

        override fun unpack(bytes: ByteArray)
            = Fieldpack.readMessagesFromByteArray(bytes)
    }
}
