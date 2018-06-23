package ru.agalkin.beholder.queue

import ru.agalkin.beholder.stats.Stats
import java.lang.ref.WeakReference

abstract class Chunk<T>(private val capacity: Int, protected val buffer: DataBuffer) {
    private val notBuffered = NotBuffered()

    @Volatile private var list = mutableListOf<T>()

    // list will be moved off to buffer, we cannot use size of that
    // also data can be dropped in the buffer, but we need to know how many items there were
    @Volatile private var nextIndexToRead = 0
    @Volatile private var size = 0

    @Volatile var droppedItemsNumber = 0
        private set

    @Volatile private var bufferCell: BufferStatus = notBuffered


    abstract fun pack(list: List<T>): WeakReference<ByteArray>
    abstract fun unpack(bufferRef: WeakReference<ByteArray>): MutableList<T>


    private fun isFull()
        = size >= capacity

    fun canAdd(): Boolean {
        synchronized(this) {
            return !isFull()
        }
    }

    fun isUsedCompletely(): Boolean {
        synchronized(this) {
            return nextIndexToRead >= size && isFull()
        }
    }

    fun add(message: T): Boolean {
        synchronized(this) {
            if (isFull()) {
                return false
            }
            list.add(message)
            size++
            return true
        }
    }

    fun next(): T? {
        synchronized(this) {
            val cell = bufferCell
            when (cell) {
                is NotBuffered -> {
                    // no need to do anything
                }
                is BufferingNow -> {
                    // cancel the buffering process
                    list = cell.list
                    bufferCell = notBuffered
                }
                is Buffered -> {
                    // the chunk is in the buffer, we need to retrieve it
                    val loadedList = unpack(cell.byteArrayReference)
                    buffer.release(cell.byteArrayReference)

                    // if the buffer dropped some of our data, we will not read all items that were packed
                    // in this case we need to report the number for stats
                    droppedItemsNumber += size - loadedList.size
                    Stats.reportQueueOverflow(droppedItemsNumber.toLong())

                    list = loadedList
                    bufferCell = notBuffered
                }
            }

            if (nextIndexToRead >= size) {
                return null
            }
            return list[nextIndexToRead++]
        }
    }

    private fun getListForBuffering(): BufferingNow? {
        synchronized(this) {
            if (nextIndexToRead != 0) {
                return null
            }
            if (bufferCell !is NotBuffered) {
                return null
            }
            val cell = BufferingNow(list)
            bufferCell = cell
            list = mutableListOf()
            return cell
        }
    }

    fun moveToBuffer() {
        val cell = getListForBuffering()
        if (cell == null) {
            return
        }

        // this can run on its own synchronization
        val reference = pack(cell.list)

        synchronized(this) {
            if (bufferCell is BufferingNow) {
                bufferCell = Buffered(reference)
            }
        }
    }


    private open inner class BufferStatus

    private inner class NotBuffered : BufferStatus()
    private inner class BufferingNow(val list: MutableList<T>) : BufferStatus()
    private inner class Buffered(val byteArrayReference: WeakReference<ByteArray>) : BufferStatus()
}
