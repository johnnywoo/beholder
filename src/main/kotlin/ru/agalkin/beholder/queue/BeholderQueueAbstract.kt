package ru.agalkin.beholder.queue

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.stats.Stats
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

abstract class BeholderQueueAbstract<T>(
    protected val app: Beholder,
    private val receive: (T) -> Received
) {
    // linked list is both Queue and List
    private val chunks = LinkedList<Chunk<T>>()

    fun getChunksOnlyForTests(): LinkedList<Chunk<T>> {
        return chunks
    }

    protected val buffer by lazy { app.defaultBuffer }

    private val totalMessagesCount = AtomicLong()

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

    abstract fun createChunk(): Chunk<T>

    private fun poll(): T? {
        synchronized(this) {
            // убираем израсходованные куски
            while (chunks.peekFirst()?.isReadable() == false) {
                val droppedChunk = chunks.pollFirst()
                val unusedItemsNumber = droppedChunk.getUnusedItemsNumber().toLong()
                totalMessagesCount.addAndGet(-unusedItemsNumber)
                Stats.reportQueueOverflow(unusedItemsNumber)
            }
            val firstChunk = chunks.peekFirst()
            if (firstChunk == null) {
                return null
            }

            val nextValue = firstChunk.next()

            if (nextValue != null) {
                totalMessagesCount.decrementAndGet()
            }

            return nextValue
        }
    }

    fun add(message: T) {
        var wasChunkAdded = false
        synchronized(this) {
            // добавляем сообщения в последний кусок
            val lastChunk = chunks.peekLast()
            val chunk: Chunk<T>
            if (lastChunk == null || !lastChunk.canAdd()) {
                // последний кусок закончился, будем добавлять новый
                chunk = createChunk()
                chunks.add(chunk)
                wasChunkAdded = true
            } else {
                chunk = lastChunk
            }

            chunk.add(message)
        }

        if (wasChunkAdded) {
            app.executor.execute {
                Stats.reportChunkCreated()
                compressChunksIfNeeded()
            }
        }

        val size = totalMessagesCount.incrementAndGet()

        Stats.reportQueueSize(size)

        executeNext()
    }

    private fun compressChunksIfNeeded() {
        synchronized(this) {
            if (chunks.size > 2) {
                val notForBuffer = setOf(chunks.peekFirst(), chunks.peekLast())
                for (chunk in chunks) {
                    if (chunk !in notForBuffer) {
                        chunk.moveToBuffer()
                    }
                }
            }
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
                        if (result == Received.OK) {
                            break
                        }
                        // Received.RETRY
                    }
                    isExecuting.set(false)
                    executeNext()
                } else {
                    isExecuting.set(false)
                }
            }
        }
    }

}
