package ru.agalkin.beholder.inflaters

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.stats.Stats

class BeholderStatsInflater : Inflater {
    private val statsHolder = Stats.createHolder()

    fun stop() {
        Stats.removeHolder(statsHolder)
    }

    override fun inflateMessageFields(message: Message, emit: (Message) -> Unit): Boolean {
        for ((k, v) in statsHolder.getFieldsAndReset()) {
            message[k] = v
        }

        emit(message)
        return true
    }
}
