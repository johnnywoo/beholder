package ru.agalkin.beholder.queue

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.config.ConfigOption
import ru.agalkin.beholder.stats.Stats
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class BeholderQueue<T : Any>(
    private val app: Beholder,
    capacityOption: ConfigOption,
    receive: (T) -> Unit
) {
    private val attemptRecieve = {
        val x = queue.poll()
        if (x != null) {
            receive(x)
        }
    }

    private val queue = LinkedBlockingQueue<T>()

    private val maxMessages = AtomicInteger(1000)

    // перед тем, как заменять конфиг приложения,
    // мы хотим поставить приём сообщений на паузу
    private val isPaused = AtomicBoolean(false)

    init {
        app.beforeReloadCallbacks.add({
            isPaused.set(true)
        })
        app.afterReloadCallbacks.add({
            isPaused.set(false)
            repeat(queue.size) {
                app.executor.execute(attemptRecieve)
            }
        })
    }

    init {
        app.afterReloadCallbacks.add {
            maxMessages.set(app.config.getIntOption(capacityOption))
        }
    }

    fun add(message: T) {
        while (queue.size >= maxMessages.get()) {
            queue.take()
            Stats.reportQueueOverflow()
        }
        queue.offer(message)
        Stats.reportQueueSize(queue.size.toLong())

        if (!isPaused.get()) {
            app.executor.execute(attemptRecieve)
        }
    }
}
