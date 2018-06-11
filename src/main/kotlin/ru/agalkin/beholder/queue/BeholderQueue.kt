package ru.agalkin.beholder.queue

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.config.ConfigOption
import ru.agalkin.beholder.stats.Stats
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class BeholderQueue<T : Any>(
    private val app: Beholder,
    capacityOption: ConfigOption,
    private val receive: (T) -> Result
) {
    private var listLength = 0
    private var head: Item? = null
    private var tail: Item? = null

    private val capacity = AtomicInteger(app.config.getIntOption(capacityOption))

    // перед тем, как заменять конфиг приложения,
    // мы хотим поставить приём сообщений на паузу
    private val isPaused = AtomicBoolean(false)

    init {
        app.beforeReloadCallbacks.add {
            isPaused.set(true)
        }
        app.afterReloadCallbacks.add {
            capacity.set(app.config.getIntOption(capacityOption))
            isPaused.set(false)
            executeNext()
        }
    }

    fun add(message: T) {
        val newItem = Item(message)

        val maxListLength = capacity.get()

        var overflows = 0
        var lengthNow = 0
        synchronized(this) {
            while (listLength >= maxListLength) {
                val item = head
                if (item != null) {
                    head = item.next
                    listLength--
                    overflows++
                }
            }

            if (listLength == 0) {
                head = newItem
            } else {
                tail!!.next = newItem
            }
            tail = newItem
            listLength++
            lengthNow = listLength
        }

        repeat(overflows) {
            Stats.reportQueueOverflow()
        }
        Stats.reportQueueSize(lengthNow.toLong())

        executeNext()
    }

    private fun poll(): T? {
        synchronized(this) {
            val headNow = head
            if (headNow == null) {
                return null
            }

            if (listLength == 1) {
                head = null
                tail = null
            } else {
                head = headNow.next
            }
            listLength--

            return headNow.value
        }
    }

    private val isExecuting = AtomicBoolean(false)

    private fun executeNext() {
        if (!isPaused.get() && !isExecuting.compareAndExchange(false, true)) {
            app.executor.execute {
                val message = poll()
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
                    isExecuting.set(false)
                    executeNext()
                } else {
                    isExecuting.set(false)
                }
            }
        }
    }

    enum class Result(val waitMillis: Long = 0) {
        OK,
        RETRY
    }

    private open inner class Item(val value: T) {
        var next: Item? = null
    }
}
