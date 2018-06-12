package ru.agalkin.beholder.queue

// This is a proof-of-concept implementation of the data buffer.
// The ultimate goal is to have a normal buffer that has a size in bytes
// which the system administrator will be able to understand better
// than sizes of queues in messages. Also, file based buffers are possible.

// If used in production, current implementation will "leak" some small
// amounts of memory whenever any queues overflow.
// You most probably will not notice that.

// This whole class must be thread-safe.
class DataBuffer {
    private val emptyList = mutableListOf<Any>()

    private val storage = mutableListOf<MutableList<*>>()

    fun <T : Item> store(list: MutableList<T>): Long {
        synchronized(this) {
            storage.add(list)
            return (storage.size - 1).toLong()
        }
    }

    fun <T : Item>load(cellId: Long): MutableList<T> {
        synchronized(this) {
            @Suppress("UNCHECKED_CAST")
            return storage.getOrNull(cellId.toInt()) as? MutableList<T> ?: mutableListOf()
        }
    }

    fun remove(cellId: Long) {
        synchronized(this) {
            // Actual removal from this list will cause renumbering
            // of items in the list, which will break our cell id scheme.
            // Normal implementation based on packing and unpacking will not have such problems.
            storage[cellId.toInt()] = emptyList
        }
    }

    interface Item
}
