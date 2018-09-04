package ru.agalkin.beholder

import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.atomic.AtomicBoolean

class MessageRouter {
    private val subscribers = CopyOnWriteArraySet<Conveyor.Input>()
    private val hasExactlyOneSubscriber = AtomicBoolean(false)

    fun addSubscriber(subscriber: Conveyor.Input) {
        synchronized(subscribers) {
            subscribers.add(subscriber)
            hasExactlyOneSubscriber.set(subscribers.size == 1)
        }
    }

    fun removeSubscriber(subscriber: Conveyor.Input) {
        synchronized(subscribers) {
            subscribers.remove(subscriber)
            hasExactlyOneSubscriber.set(subscribers.size == 1)
        }
    }

    fun hasSubscribers()
        = !subscribers.isEmpty()

    fun sendMessageToSubscribers(message: Message) {
        // Если получатель один, копировать сообщение незачем, пусть меняет inplace.
        // Тогда мы не будем плодить 5 копий, когда сообщение проходит по очереди через 5 команд.
        if (hasExactlyOneSubscriber.get()) {
            for (subscriber in subscribers) {
                subscriber.addMessage(message)
            }
            return
        }

        // Eсли получателей несколько, нужно рассылать копии сообщений,
        // чтобы изменения из одного обработчика не пересекались с изменениями в других.
        for (subscriber in subscribers) {
            subscriber.addMessage(message.copy())
        }
    }

    fun sendUniqueMessagesToSubscribers(producer: () -> Message) {
        for (subscriber in subscribers) {
            subscriber.addMessage(producer())
        }
    }
}
