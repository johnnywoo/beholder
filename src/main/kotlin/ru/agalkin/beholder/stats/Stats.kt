package ru.agalkin.beholder.stats

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

    fun reportQueueOverflow() {
        for (holder in holders) {
            holder.reportQueueOverflow()
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
}
