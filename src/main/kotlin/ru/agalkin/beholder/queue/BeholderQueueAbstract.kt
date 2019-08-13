package ru.agalkin.beholder.queue

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.removeMatching
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

    fun getIsPausedOnlyForTests(): AtomicBoolean {
        return isPaused
    }

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
            cleanupDroppedChunks()

            val firstChunk = chunks.peekFirst()
            if (firstChunk == null) {
                return null
            }

            val nextValue = firstChunk.next()

            if (nextValue != null) {
                val size = totalMessagesCount.decrementAndGet()
                Stats.reportQueueSize(size)
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
            // Убираем дохлые чанки, чтобы меньше накапливать фигни.
            cleanupDroppedChunks()

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
                // Убираем дохлые чанки, потому что moveToBuffer() мог вытеснить старые данные из буфера.
                cleanupDroppedChunks()
            }
        }
    }

    private fun cleanupDroppedChunks() {
        // убираем израсходованные куски
        chunks.removeMatching {
            if (!it.isReadable()) {
                val unusedItemsNumber = it.getUnusedItemsNumber().toLong()
                val size = totalMessagesCount.addAndGet(-unusedItemsNumber)
                Stats.reportQueueSize(size)
                Stats.reportQueueOverflow(unusedItemsNumber)
                return@removeMatching true
            }
            return@removeMatching false
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
