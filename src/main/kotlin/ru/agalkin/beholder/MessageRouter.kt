package ru.agalkin.beholder

import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean

class MessageRouter {
    private val subscribers = CopyOnWriteArraySet<(Message) -> Unit>()
    private val hasExactlyOneSubscriber = AtomicBoolean(false)

    fun addSubscriber(subscriber: (Message) -> Unit) {
        synchronized(subscribers) {
            subscribers.add(subscriber)
            hasExactlyOneSubscriber.set(subscribers.size == 1)
        }
    }

    fun removeSubscriber(subscriber: (Message) -> Unit) {
        synchronized(subscribers) {
            subscribers.remove(subscriber)
            hasExactlyOneSubscriber.set(subscribers.size == 1)
        }
    }

    fun hasSubscribers()
        = !subscribers.isEmpty()

    fun sendMessageToSubscribers(message: Message) {
        // если получатель один, копировать сообщение незачем, пусть меняет inplace
        // тогда мы не будем плодить 5 копий, когда сообщение проходит по очереди через 5 команд
        if (hasExactlyOneSubscriber.get()) {
            for (subscriber in subscribers) {
                subscriber(message)
            }
            return
        }

        // если получателей несколько, нужно рассылать копии сообщений,
        // чтобы изменения из одного обработчика не пересекались с изменениями в других
        for (subscriber in subscribers) {
            subscriber(message.copy())
        }
    }

    fun sendUniqueMessagesToSubscribers(producer: () -> Message) {
        for (subscriber in subscribers) {
            subscriber(producer())
        }
    }
}
