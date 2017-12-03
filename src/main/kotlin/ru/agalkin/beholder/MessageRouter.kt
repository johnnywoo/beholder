package ru.agalkin.beholder

import java.util.concurrent.CopyOnWriteArraySet

class MessageRouter {
    val subscribers = CopyOnWriteArraySet<(Message) -> Unit>()

    fun sendMessageToSubscribers(message: Message) {
        var isCopyNeeded = false
        for (subscriber in subscribers) {
            subscriber(if (isCopyNeeded) message else message.copy())
            isCopyNeeded = true
        }
    }

    fun sendUniqueMessagesToSubscribers(producer: () -> Message) {
        for (subscriber in subscribers) {
            subscriber(producer())
        }
    }
}
