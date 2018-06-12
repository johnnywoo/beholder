package ru.agalkin.beholder.queue

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.config.ConfigOption
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class BeholderQueue<T : Any>(
    private val app: Beholder,
    capacityOption: ConfigOption,
    private val receive: (T) -> Result
) {
    private val chunks: MutableList<Chunk> = LinkedList()

    // перед тем, как заменять конфиг приложения,
    // мы хотим поставить приём сообщений на паузу
    private val isPaused = AtomicBoolean(false)

    init {
        app.beforeReloadCallbacks.add {
            isPaused.set(true)
        }
        app.afterReloadCallbacks.add {
            isPaused.set(false)
            executeNext()
        }
    }

    fun add(message: T) {
        InternalLog.info("Adding (1) $message")
        synchronized(this) {
            InternalLog.info("Adding (2) $message")
            // добавляем сообщения в последний кусок
            val lastChunk = chunks.lastOrNull()
            val chunk: Chunk
            if (lastChunk == null || !lastChunk.canAdd()) {
                // последний кусок закончился, будем добавлять новый
                chunk = Chunk(300)
                chunks.add(chunk)
            } else {
                chunk = lastChunk
            }

            InternalLog.info("Adding (3) $message")
            chunk.add(message)
            InternalLog.info("Added (3) $message")
        }

        // todo decide what to do here
//        repeat(overflows) {
//            Stats.reportQueueOverflow()
//        }
//        Stats.reportQueueSize(lengthNow.toLong())

        executeNext()
    }

    private fun poll(): T? {
        synchronized(this) {
            // берём сообщения из первого куска
            val firstChunk = chunks.firstOrNull()
            if (firstChunk == null) {
                return null
            }
            val value = firstChunk.shift()
            if (firstChunk.isUsedCompletely()) {
                chunks.removeAt(0)
            }
            return value
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

    private inner class Chunk(private val capacity: Int) {
        private val list = mutableListOf<T>()
        private var index = 0

        fun canAdd()
            = list.size < capacity

        fun isUsedCompletely()
            = index >= list.size && !canAdd()

        fun add(message: T): Boolean {
            if (!canAdd()) {
                return false
            }
            list.add(message)
            return true
        }

        fun shift(): T? {
            if (index >= list.size) {
                return null
            }
            return list[index++]
        }
    }
}
