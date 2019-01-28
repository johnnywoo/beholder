package ru.agalkin.beholder.senders

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.FieldValue
import ru.agalkin.beholder.queue.FieldValueQueue
import ru.agalkin.beholder.queue.Received
import java.util.*

class MockSender(app: Beholder) {
    val received = LinkedList<FieldValue>()

    private val queue = FieldValueQueue(app) { fieldValue ->
        received.add(fieldValue)
        return@FieldValueQueue Received.OK
    }

    fun writeMessagePayload(fieldValue: FieldValue) {
        queue.add(fieldValue)
    }
}
