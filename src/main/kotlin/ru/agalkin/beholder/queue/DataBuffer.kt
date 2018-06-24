package ru.agalkin.beholder.queue

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.config.ConfigOption
import ru.agalkin.beholder.stats.Stats
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class DataBuffer(app: Beholder, val id: String = "") {
    private val maxTotalSize = AtomicInteger(app.getIntOption(ConfigOption.BUFFER_MEMORY_BYTES))
    @Volatile var compressionName = app.getCompressionOption(ConfigOption.BUFFER_COMPRESSION)
    @Volatile var compressor = app.createCompressor(ConfigOption.BUFFER_COMPRESSION)

    init {
        app.afterReloadCallbacks.add {
            maxTotalSize.set(app.getIntOption(ConfigOption.BUFFER_MEMORY_BYTES))
            if (compressionName != app.getCompressionOption(ConfigOption.BUFFER_COMPRESSION)) {
                compressionName = app.getCompressionOption(ConfigOption.BUFFER_COMPRESSION)
                compressor = app.createCompressor(ConfigOption.BUFFER_COMPRESSION)
            }
        }
    }

    val currentSizeInMemory = AtomicLong(0)

    private val byteArrays: Deque<ByteArray> = LinkedList<ByteArray>()

    private fun addMemorySize(n: Int) {
        val cur = currentSizeInMemory.addAndGet(n.toLong())
        val all = allBuffersMemoryBytes.addAndGet(n.toLong())
        if (n > 0) {
            Stats.reportBufferAllocation(this, cur, all, n.toLong())
        }
    }

    fun allocate(bytes: ByteArray): WeakReference<ByteArray> {
        synchronized(this) {
            while (currentSizeInMemory.get() + bytes.size > maxTotalSize.get()) {
                val removed = byteArrays.pollFirst()
                addMemorySize(-removed.size)
            }

            byteArrays.addLast(bytes)
            addMemorySize(bytes.size)

            return WeakReference(bytes)
        }
    }

    fun release(ref: WeakReference<ByteArray>)
        = release(ref.get())

    private fun release(byteArray: ByteArray?) {
        if (byteArray == null) {
            return
        }
        synchronized(this) {
            val isRemoved = byteArrays.remove(byteArray)
            if (isRemoved) {
                addMemorySize(-byteArray.size)
            }
        }
    }

    companion object {
        val allBuffersMemoryBytes = AtomicLong(0)
    }
}
