package ru.agalkin.beholder.queue

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.config.ConfigOption
import ru.agalkin.beholder.stats.Stats
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

private const val NOT_BUFFERED  = -2L
private const val BUFFERING_NOW = -1L

class BeholderQueue<T : DataBuffer.Item>(
    private val app: Beholder,
    private val receive: (T) -> Result
) {
    // linked list is both Queue and List
    private val chunks = LinkedList<Chunk>()

    private val buffer = app.defaultBuffer

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

    private fun poll(): T? {
        synchronized(this) {
            // берём сообщения из первого куска
            while (chunks.peekFirst()?.isUsedCompletely() == true) {
                chunks.poll()
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
            val chunk: Chunk
            if (lastChunk == null || !lastChunk.canAdd()) {
                // последний кусок закончился, будем добавлять новый
                chunk = Chunk(app.config.getIntOption(ConfigOption.QUEUE_CHUNK_MESSAGES))
                chunks.add(chunk)
                wasChunkAdded = true
            } else {
                chunk = lastChunk
            }

            chunk.add(message)
        }

        if (wasChunkAdded) {
            app.executor.execute {
                compressChunksIfNeeded()
            }
        }

        val size = totalMessagesCount.incrementAndGet()

        Stats.reportQueueSize(size)

        executeNext()
    }

    private fun compressChunksIfNeeded() {
        if (chunks.size > 2) {
            val notForBuffer = setOf(chunks.peekFirst(), chunks.peekLast())
            for (chunk in chunks) {
                if (chunk !in notForBuffer) {
                    chunk.moveToBuffer()
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
        @Volatile private var list = mutableListOf<T>()

        @Volatile private var index = 0
        @Volatile private var size = 0 // list will be moved off to buffer

        @Volatile private var bufferCellId = NOT_BUFFERED

        @Volatile private var listForBuffer = mutableListOf<T>()


        // called from the queue itself, already synchronized on it
        fun canAdd()
            = size < capacity

        // called from the queue itself, already synchronized on it
        fun isUsedCompletely()
            = index >= size && !canAdd()

        // called from the queue itself, already synchronized on it
        fun add(message: T): Boolean {
            if (!canAdd()) {
                return false
            }
            list.add(message)
            size++
            return true
        }

        // called from the queue itself, already synchronized on it
        fun next(): T? {
            when (bufferCellId) {
                NOT_BUFFERED -> {
                    // no need to do anything
                }
                BUFFERING_NOW -> {
                    // cancel the buffering process
                    list = listForBuffer
                    listForBuffer = mutableListOf()
                    bufferCellId = NOT_BUFFERED
                }
                else -> {
                    // the chunk is in the buffer, we need to retrieve it
                    val loadedList = buffer.load<T>(bufferCellId)

                    val cellIdToRemove = bufferCellId
                    app.executor.execute {
                        buffer.remove(cellIdToRemove)
                    }

                    list = loadedList
                    bufferCellId = NOT_BUFFERED
                }
            }

            if (index >= size) {
                return null
            }
            return list[index++]
        }

        // Called from Executor, synchronization needed
        fun moveToBuffer() {
            synchronized(this@BeholderQueue) {
                if (index != 0) {
                    return
                }
                if (bufferCellId != NOT_BUFFERED) {
                    return
                }
                listForBuffer = list
                list = mutableListOf()
                bufferCellId = BUFFERING_NOW
            }

            // this can run on its own synchronization
            val cellId = buffer.store(listForBuffer)

            synchronized(this@BeholderQueue) {
                if (bufferCellId == BUFFERING_NOW) {
                    bufferCellId = cellId
                    listForBuffer = mutableListOf()
                }
            }
        }
    }
}
