package ru.agalkin.beholder.queue

import java.lang.ref.WeakReference
import kotlin.math.max

abstract class Chunk<T>(private val capacity: Int, protected val buffer: DataBuffer) {
    private val notBuffered = NotBuffered()

    @Volatile private var list = mutableListOf<T>()

    // list will be moved off to buffer, we cannot use size of that
    // also data can be dropped in the buffer, but we need to know how many items there were
    @Volatile private var nextIndexToRead = 0
    @Volatile private var addedItemsCount = 0
    @Volatile private var droppedItemsCount = 0

    @Volatile private var bufferState: BufferState = notBuffered


    abstract fun pack(list: List<T>): WeakReference<ByteArray>
    abstract fun unpack(bufferRef: WeakReference<ByteArray>): MutableList<T>


    fun getUnusedItemsNumber(): Int {
        synchronized(this) {
            return droppedItemsCount + max(0, addedItemsCount - nextIndexToRead)
        }
    }

    private fun isFull()
        = addedItemsCount >= capacity

    fun canAdd(): Boolean {
        synchronized(this) {
            return !isFull()
        }
    }

    fun isUsedCompletely(): Boolean {
        synchronized(this) {
            return nextIndexToRead >= addedItemsCount && isFull()
        }
    }

    fun add(message: T): Boolean {
        synchronized(this) {
            if (isFull()) {
                return false
            }
            list.add(message)
            addedItemsCount++
            return true
        }
    }

    fun next(): T? {
        synchronized(this) {
            val cell = bufferState
            when (cell) {
                is NotBuffered -> {
                    // no need to do anything
                }
                is BufferingNow -> {
                    // cancel the buffering process
                    list = cell.list
                    bufferState = notBuffered
                }
                is Buffered -> {
                    // the chunk is in the buffer, we need to retrieve it
                    val loadedList = unpack(cell.byteArrayReference)
                    buffer.release(cell.byteArrayReference)

                    // if the buffer dropped some of our data, we will not read all items that were packed
                    // in this case we need to report the number for stats
                    droppedItemsCount += addedItemsCount - loadedList.size

                    list = loadedList
                    bufferState = notBuffered
                }
            }

            if (nextIndexToRead >= addedItemsCount) {
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
            if (bufferState !is NotBuffered) {
                return null
            }
            val cell = BufferingNow(list)
            bufferState = cell
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
            if (bufferState is BufferingNow) {
                bufferState = Buffered(reference)
            }
        }
    }


    private open inner class BufferState

    private inner class NotBuffered : BufferState()
    private inner class BufferingNow(val list: MutableList<T>) : BufferState()
    private inner class Buffered(val byteArrayReference: WeakReference<ByteArray>) : BufferState()
}
