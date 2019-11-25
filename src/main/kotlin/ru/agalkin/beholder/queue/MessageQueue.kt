package ru.agalkin.beholder.queue

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.Fieldpack
import ru.agalkin.beholder.Message

class MessageQueue(app: Beholder, receive: (Message) -> Received) : BeholderQueueAbstract<Message>(app, receive) {
    override fun pack(list: List<Message>)
        = Fieldpack.writeMessagesToByteArray(list)

    override fun unpack(bytes: ByteArray)
        = Fieldpack.readMessagesFromByteArray(bytes)
}
