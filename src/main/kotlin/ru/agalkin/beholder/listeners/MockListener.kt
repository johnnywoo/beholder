package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.MessageRouter
import ru.agalkin.beholder.queue.MessageQueue
import ru.agalkin.beholder.queue.Received

class MockListener(app: Beholder) {
    val router = MessageRouter()

    val queue = MessageQueue(app) {
        router.sendMessageToSubscribers(it)
        Received.OK
    }
}
