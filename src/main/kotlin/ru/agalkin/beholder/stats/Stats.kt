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
}
