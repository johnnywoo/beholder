package ru.agalkin.beholder.queue

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.config.ConfigOption
import ru.agalkin.beholder.stats.Stats
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

const val DEFAULT_DATA_BUFFER_SIZE = 128 * 1024 * 1024
val DEFAULT_DATA_BUFFER_COMPRESSION = ConfigOption.Compression.LZ4_FAST

class DataBuffer(private val app: Beholder, val id: String = "") {
    private val maxTotalSize = AtomicInteger(DEFAULT_BUFFER_SIZE)
    @Volatile private var compressionName = DEFAULT_DATA_BUFFER_COMPRESSION
    @Volatile var compressor = app.createCompressorByName(compressionName)

    val currentSizeInMemory = AtomicLong(0)

    fun setMemoryBytes(size: Int) {
        maxTotalSize.set(size)
    }

    fun setCompression(newCompressionName: ConfigOption.Compression) {
        if (compressionName != newCompressionName) {
            compressionName = newCompressionName
            compressor = app.createCompressorByName(newCompressionName)
        }
    }

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
                if (removed != null) {
                    addMemorySize(-removed.size)
                }
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

    fun getByteArraysForTestingOnly(): Deque<ByteArray> {
        return byteArrays
    }

    companion object {
        val allBuffersMemoryBytes = AtomicLong(0)
    }
}
