package ru.agalkin.beholder.queue

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.stats.Stats
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

abstract class BeholderQueueAbstract<T>(
    protected val app: Beholder,
    private val receive: (T) -> Received
) {
    private val chunks = CopyOnWriteArrayList<Chunk<T>>()

    fun getChunksOnlyForTests(): List<Chunk<T>> {
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
            removeDroppedChunksFromHead()

            val firstChunk = chunks.firstOrNull()
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
            val lastChunk = chunks.lastOrNull()
            val chunk: Chunk<T>
            if (lastChunk == null || !lastChunk.canAdd()) {
                // последний кусок закончился, будем добавлять новый
                chunk = createChunk()
                chunks.add(chunk)

                wasChunkAdded = true

                // Убираем дохлые чанки, чтобы меньше накапливать фигни.
                removeBufferableDroppedChunks()
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
                val notForBuffer = setOf(chunks.firstOrNull(), chunks.lastOrNull())
                for (chunk in chunks) {
                    if (chunk !in notForBuffer) {
                        chunk.moveToBuffer()
                    }
                }
                // Убираем дохлые чанки, потому что moveToBuffer() мог вытеснить старые данные из буфера.
                removeBufferableDroppedChunks()
            }
        }
    }

    private fun removeDroppedChunksFromHead() {
        cleanupDroppedChunks(0)
    }

    private fun removeBufferableDroppedChunks() {
        cleanupDroppedChunks(1)
    }

    private tailrec fun cleanupDroppedChunks(index: Int) {
        if (chunks.size <= index) {
            return
        }
        val chunk = chunks[index]
        if (!chunk.isReadable()) {
            chunks.removeAt(index)

            val unusedItemsNumber = chunk.getUnusedItemsNumber().toLong()
            val size = totalMessagesCount.addAndGet(-unusedItemsNumber)
            Stats.reportQueueSize(size)
            Stats.reportQueueOverflow(unusedItemsNumber)

            cleanupDroppedChunks(index)
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
