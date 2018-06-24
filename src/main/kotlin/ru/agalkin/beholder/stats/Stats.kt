package ru.agalkin.beholder.stats

import ru.agalkin.beholder.queue.DataBuffer
import java.util.concurrent.CopyOnWriteArraySet

object Stats {
    private val holders = CopyOnWriteArraySet<StatsHolder>()

    fun createHolder(): StatsHolder {
        val statsHolder = StatsHolder()
        holders.add(statsHolder)
        return statsHolder
    }

    fun removeHolder(holder: StatsHolder) {
        holders.remove(holder)
    }

    fun reportUdpReceived(size: Long) {
        for (holder in holders) {
            holder.reportUdpReceived(size)
        }
    }

    fun reportTcpReceived(size: Long) {
        for (holder in holders) {
            holder.reportTcpReceived(size)
        }
    }

    fun reportTcpConnected() {
        for (holder in holders) {
            holder.reportTcpConnected()
        }
    }

    fun reportQueueOverflow(droppedNumber: Long) {
        for (holder in holders) {
            holder.reportQueueOverflow(droppedNumber)
        }
    }

    fun reportQueueSize(size: Long) {
        for (holder in holders) {
            holder.reportQueueSize(size)
        }
    }

    fun reportUnparsedDropped() {
        for (holder in holders) {
            holder.reportUnparsedDropped()
        }
    }

    fun reportReload() {
        for (holder in holders) {
            holder.reportConfigReload()
        }
    }

    fun reportChunkCreated() {
        for (holder in holders) {
            holder.reportChunkCreated()
        }
    }

    fun reportBufferAllocation(buffer: DataBuffer, currentMemoryBytes: Long, allBuffersMemoryBytes: Long, allocatedBytes: Long) {
        for (holder in holders) {
            holder.reportBufferAllocation(buffer, currentMemoryBytes, allBuffersMemoryBytes, allocatedBytes)
        }
    }

    inline fun <T> timePackProcess(block: () -> T): T {
        val startNanos = System.nanoTime()
        val result = block()
        val spentMillis = System.nanoTime() - startNanos
        reportPackTime(spentMillis)
        return result
    }

    fun reportPackTime(spentMillis: Long) {
        for (holder in holders) {
            holder.reportPackTime(spentMillis)
        }
    }

    inline fun timeCompressProcess(originalLength: Int, block: () -> ByteArray): ByteArray {
        val startNanos = System.nanoTime()
        val result = block()
        val spentNanos = System.nanoTime() - startNanos
        reportCompressTime(spentNanos, originalLength, result.size)
        return result
    }

    fun reportCompressTime(spentNanos: Long, originalLength: Int, compressedLength: Int) {
        for (holder in holders) {
            holder.reportCompressTime(spentNanos, originalLength, compressedLength)
        }
    }

    inline fun <T> timeUnpackProcess(block: () -> T): T {
        val startNanos = System.nanoTime()
        val result = block()
        val spentNanos = System.nanoTime() - startNanos
        reportUnpackTime(spentNanos)
        return result
    }

    fun reportUnpackTime(spentNanos: Long) {
        for (holder in holders) {
            holder.reportUnpackTime(spentNanos)
        }
    }

    inline fun <T> timeDecompressProcess(block: () -> T): T {
        val startNanos = System.nanoTime()
        val result = block()
        val spentNanos = System.nanoTime() - startNanos
        reportDecompressTime(spentNanos)
        return result
    }

    fun reportDecompressTime(spentNanos: Long) {
        for (holder in holders) {
            holder.reportDecompressTime(spentNanos)
        }
    }
}
