package ru.agalkin.beholder.queue

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.Fieldpack
import ru.agalkin.beholder.Message
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
    private val maxTotalSize = AtomicInteger(app.config.getIntOption(ConfigOption.BUFFER_MEMORY_BYTES))
    init {
        app.afterReloadCallbacks.add {
            maxTotalSize.set(app.config.getIntOption(ConfigOption.BUFFER_MEMORY_BYTES))
        }
    }

    val currentSizeInMemory = AtomicInteger(0)

    fun <T> store(list: List<T>): Cell<T> {
        synchronized(this) {
            val cell = when (list.firstOrNull()) {
                is Message -> @Suppress("UNCHECKED_CAST") FieldpackCell(list as List<Message>)
                is FieldValue -> @Suppress("UNCHECKED_CAST") BlobizedCell(list as List<FieldValue>)
                else -> ListCell(list)
            }
            currentSizeInMemory.addAndGet(cell.getMemoryUsedBytes())
            @Suppress("UNCHECKED_CAST")
            return cell as Cell<T>
        }
    }

    fun <T> load(cell: Cell<T>): List<T> {
        synchronized(this) {
            return cell.getList()
        }
    }

    fun <T> remove(cell: Cell<T>) {
        synchronized(this) {
            currentSizeInMemory.addAndGet(-cell.getMemoryUsedBytes())
        }
    }


    interface Cell<T> {
        fun getList(): List<T>
        fun getMemoryUsedBytes(): Int
    }

    private class ListCell<T>(private val items: List<T>) : Cell<T> {
        override fun getList()
            = items

        override fun getMemoryUsedBytes()
            = 0
    }

    private class FieldpackCell(items: List<Message>) : Cell<Message> {
        private val bytes: ByteArray
        init {
            val fieldpack = Fieldpack()
            bytes = ByteArray(fieldpack.getPackedLength(items))
            var offset = 0
            fieldpack.writeMessages(items) {source, length ->
                for (i in 0 until length) {
                    bytes[offset + i] = source[i]
                }
                offset += length
            }
        }

        override fun getList(): List<Message> {
            var offset = 0
            return Fieldpack().readMessages { length ->
                val stringValue = Fieldpack.Portion(bytes, offset, length)
                offset += length
                stringValue
            }
        }

        override fun getMemoryUsedBytes()
            = bytes.size
    }

    private class BlobizedCell(items: List<FieldValue>) : Cell<FieldValue> {
        private val bytes: ByteArray
        init {
            val fieldpack = Fieldpack()
            var length = 0
            for (item in items) {
                val byteLength = item.getByteLength()
                length += fieldpack.writeNum(byteLength, {_,_->}) + byteLength
            }

            bytes = ByteArray(length)

            var offset = 0
            for (item in items) {
                val byteLength = item.getByteLength()
                fieldpack.writeNum(byteLength, { source, readLength ->
                    for (i in 0 until readLength) {
                        bytes[offset + i] = source[i]
                    }
                    offset += readLength
                })
                val ba = item.toByteArray()
                for (i in 0 until byteLength) {
                    bytes[offset + i] = ba[i]
                }
                offset += byteLength
            }
        }

        override fun getList(): List<FieldValue> {
            val fieldpack = Fieldpack()

            val list = mutableListOf<FieldValue>()
            var offset = 0
            while (offset < bytes.size) {
                val byteLength = (fieldpack.readNum { length ->
                    val portion = Fieldpack.Portion(bytes, offset, length)
                    offset += length
                    portion
                }).toInt()
                list.add(FieldValue.fromByteArray(
                    bytes.copyOfRange(offset, byteLength),
                    byteLength
                ))
                offset += byteLength
            }
            return list
        }

        override fun getMemoryUsedBytes()
            = bytes.size
    }
}
