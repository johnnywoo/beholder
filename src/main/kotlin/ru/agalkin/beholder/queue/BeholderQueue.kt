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
    private val receive: (T) -> Result
) {
    private val queue = LinkedBlockingQueue<T>()

    private val capacity = AtomicInteger(app.config.getIntOption(capacityOption))

    // перед тем, как заменять конфиг приложения,
    // мы хотим поставить приём сообщений на паузу
    private val isPaused = AtomicBoolean(false)

    init {
        app.beforeReloadCallbacks.add({
            isPaused.set(true)
        })
        app.afterReloadCallbacks.add({
            isPaused.set(false)
            executeNext()
        })
    }

    init {
        app.afterReloadCallbacks.add {
            capacity.set(app.config.getIntOption(capacityOption))
        }
    }

    fun add(message: T) {
        while (queue.size >= capacity.get()) {
            queue.take()
            Stats.reportQueueOverflow()
        }
        queue.offer(message)
        Stats.reportQueueSize(queue.size.toLong())

        executeNext()
    }

    private val isExecuting = AtomicBoolean(false)

    private fun executeNext() {
        if (!isPaused.get() && !isExecuting.compareAndExchange(false, true)) {
            app.executor.execute {
                val message = queue.poll()
                if (message != null) {
                    while (true) {
                        val result = receive(message)
                        if (result == Result.OK) {
                            break
                        }
                        // Result.RETRY
                        if (result.waitMillis > 0) {
                            Thread.sleep(result.waitMillis)
                        }
                    }
                }
                isExecuting.set(false)
                if (!queue.isEmpty()) {
                    executeNext()
                }
            }
        }
    }

    enum class Result(val waitMillis: Long = 0) {
        OK,
        RETRY
    }
}
