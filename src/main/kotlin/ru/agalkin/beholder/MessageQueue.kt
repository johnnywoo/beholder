package ru.agalkin.beholder

import ru.agalkin.beholder.config.ConfigOption
import ru.agalkin.beholder.stats.Stats
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class MessageQueue(app: Beholder, capacityOption: ConfigOption) {
    private val queue = LinkedBlockingQueue<Message>()

    private val maxMessages = AtomicInteger(1000)

    init {
        app.afterReloadCallbacks.add {
            maxMessages.set(app.config.getIntOption(capacityOption))
        }
    }

    fun add(message: Message) {
        while (queue.size >= maxMessages.get()) {
            queue.take()
            Stats.reportQueueOverflow()
        }
        queue.offer(message)
        Stats.reportQueueSize(queue.size.toLong())
    }

    fun shift(timeoutMillis: Long): Message? {
        return queue.poll(timeoutMillis, TimeUnit.MILLISECONDS)
    }

    fun destroy() {
        queue.clear()
    }
}
