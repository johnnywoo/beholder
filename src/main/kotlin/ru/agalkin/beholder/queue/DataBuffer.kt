package ru.agalkin.beholder.queue

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.BeholderException
import ru.agalkin.beholder.compressors.Compressor
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

    private val allocatedParts: Deque<Buffered> = ArrayDeque<Buffered>()

    fun getDataOnlyForTests(): Deque<Buffered> {
        return allocatedParts
    }

    private fun addMemorySize(n: Int) {
        val cur = currentSizeInMemory.addAndGet(n.toLong())
        val all = allBuffersMemoryBytes.addAndGet(n.toLong())
        Stats.reportBufferSizeChange(this, cur, all, n.toLong())
    }

    fun allocate(compressedBytes: ByteArray, originalSize: Int, compressor: Compressor, onDiscard: () -> Unit): WeakReference<Buffered> {
        val allocatedSize = compressedBytes.size
        val totalSize = maxTotalSize.get()
        if (allocatedSize > totalSize) {
            throw BeholderException("Trying to allocate $allocatedSize bytes which is bigger than the whole buffer ($totalSize bytes)")
        }
        synchronized(this) {
            while (currentSizeInMemory.get() + allocatedSize > totalSize) {
                val removed = allocatedParts.pollFirst()
                if (removed != null) {
                    removed.onDiscard()
                    addMemorySize(-removed.allocatedSize)
                }
            }

            val buffered = Buffered(compressedBytes, originalSize, allocatedSize, compressor, onDiscard)

            allocatedParts.addLast(buffered)
            addMemorySize(allocatedSize)

            return WeakReference(buffered)
        }
    }

    private fun releaseData(buffered: Buffered) {
        synchronized(this) {
            val isRemoved = allocatedParts.remove(buffered)
            if (isRemoved) {
                addMemorySize(-buffered.allocatedSize)
            }
        }
    }

    inner class Buffered(
        val compressedBytes: ByteArray,
        val originalSize: Int,
        val allocatedSize: Int,
        val compressor: Compressor,
        val onDiscard: () -> Unit
    ) {
        fun release(): ByteArray {
            releaseData(this)
            return compressedBytes
        }
    }

    companion object {
        val allBuffersMemoryBytes = AtomicLong(0)
    }
}
