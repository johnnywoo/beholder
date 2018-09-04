package ru.agalkin.beholder.inflaters

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.stats.Stats

class BeholderStatsInflater : InplaceInflater {
    private val statsHolder = Stats.createHolder()

    fun stop() {
        Stats.removeHolder(statsHolder)
    }

    override fun inflateMessageFieldsInplace(message: Message): Boolean {
        for ((k, v) in statsHolder.getFieldsAndReset()) {
            message[k] = v
        }

        return true
    }
}
