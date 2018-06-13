package ru.agalkin.beholder.queue

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.config.ConfigOption
import java.util.concurrent.atomic.AtomicInteger

// This is a proof-of-concept implementation of the data buffer.
// The ultimate goal is to have a normal buffer that has a size in bytes
// which the system administrator will be able to understand better
// than sizes of queues in messages. Also, file based buffers are possible.

// If used in production, current implementation will "leak" some small
// amounts of memory whenever any queues overflow.
// You most probably will not notice that.

// This whole class must be thread-safe.
class DataBuffer(app: Beholder) {
    private val maxTotalSize = AtomicInteger(app.config.getIntOption(ConfigOption.BUFFER_SIZE_BYTES))
    init {
        app.afterReloadCallbacks.add {
            maxTotalSize.set(app.config.getIntOption(ConfigOption.BUFFER_SIZE_BYTES))
        }
    }

    private val currentSizeInMemory = AtomicInteger(0)

    fun <T : Item> store(list: List<T>): Cell<T> {
        val cell = ListCell(list)
        currentSizeInMemory.addAndGet(cell.getMemoryUsedBytes())
        return cell
    }

    fun <T : Item> load(cell: Cell<T>): List<T> {
        return cell.getList()
    }

    fun <T : Item> remove(cell: Cell<T>) {
        currentSizeInMemory.addAndGet(-cell.getMemoryUsedBytes())
    }


    interface Cell<T : Item> {
        fun getList(): List<T>
        fun getMemoryUsedBytes(): Int
    }

    class ListCell<T : Item>(private val items: List<T>) : Cell<T> {
        override fun getList()
            = items

        override fun getMemoryUsedBytes()
            = 0
    }

    interface Item
}
