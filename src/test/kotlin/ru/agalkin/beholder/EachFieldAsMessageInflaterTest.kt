package ru.agalkin.beholder

import org.junit.Test
import kotlin.test.assertEquals

class EachFieldAsMessageInflaterTest : TestAbstract() {
    @Test
    fun testEachFieldAsMessageInflater() {
        val received = receiveMessagesWithConfig("parse each-field-as-message", 2) {
            val message = Message()
            message["cat"] = "feline"
            message["dog"] = "canine"
            it.input(message)
        }

        assertFieldValues(received[0], mapOf(
            "key" to "cat",
            "value" to "feline"
        ))

        assertFieldValues(received[1], mapOf(
            "key" to "dog",
            "value" to "canine"
        ))
    }

    @Test
    fun testStats() {
        val received = receiveMessagesWithConfig(
            "parse beholder-stats",
            1,
            { it.input(Message()) }
        )

        assertFieldNames(
            received[0],
            "fromTcpMaxBytes", "fromTcpMessages", "fromTcpNewConnections", "fromTcpTotalBytes",
            "fromUdpMaxBytes", "fromUdpMessages", "fromUdpTotalBytes",
            "heapBytes", "heapMaxBytes", "heapUsedBytes",
            "packCount", "packDurationMaxNanos", "packDurationTotalNanos",
            "unpackCount", "unpackDurationMaxNanos", "unpackDurationTotalNanos",
            "compressCount", "compressDurationMaxNanos", "compressDurationTotalNanos", "compressBeforeTotalBytes", "compressAfterTotalBytes",
            "decompressCount", "decompressDurationMaxNanos", "decompressDurationTotalNanos",
            "queueMaxSize", "queueOverflows", "queueChunksCreated", "uptimeSeconds", "messagesReceived", "configReloads", "unparsedDropped",
            "allBuffersAllocatedBytes", "allBuffersMaxBytes", "defaultBufferAllocatedBytes", "defaultBufferMaxBytes",
            "payload"
        )
    }

    @Test
    fun testStatsInfluxizer() {
        val received = receiveMessagesWithConfig(
            """
                parse beholder-stats;
                parse each-field-as-message;
                switch ¥value { case ~^[0-9]+¥~ {} }
                set ¥payload 'beholder,tag=tagval ¥key=¥value';
            """.replace('¥', '$'),
            35,
            {
                val message = Message()
                message["date"]    = "2017-11-26T16:16:01+03:00"
                message["payload"] = "lots of irrelevant words"
                it.input(message)
            }
        )

        val lineProtocolPackets = received
            .map { it.getPayloadString().replace("[0-9]+".toRegex(), "N") }
            .sorted()

        assertEquals(listOf(
            "beholder,tag=tagval allBuffersAllocatedBytes=N",
            "beholder,tag=tagval allBuffersMaxBytes=N",
            "beholder,tag=tagval compressAfterTotalBytes=N",
            "beholder,tag=tagval compressBeforeTotalBytes=N",
            "beholder,tag=tagval compressCount=N",
            "beholder,tag=tagval compressDurationMaxNanos=N",
            "beholder,tag=tagval compressDurationTotalNanos=N",
            "beholder,tag=tagval configReloads=N",
            "beholder,tag=tagval decompressCount=N",
            "beholder,tag=tagval decompressDurationMaxNanos=N",
            "beholder,tag=tagval decompressDurationTotalNanos=N",
            "beholder,tag=tagval defaultBufferAllocatedBytes=N",
            "beholder,tag=tagval defaultBufferMaxBytes=N",
            "beholder,tag=tagval fromTcpMaxBytes=N",
            "beholder,tag=tagval fromTcpMessages=N",
            "beholder,tag=tagval fromTcpNewConnections=N",
            "beholder,tag=tagval fromTcpTotalBytes=N",
            "beholder,tag=tagval fromUdpMaxBytes=N",
            "beholder,tag=tagval fromUdpMessages=N",
            "beholder,tag=tagval fromUdpTotalBytes=N",
            "beholder,tag=tagval heapBytes=N",
            "beholder,tag=tagval heapMaxBytes=N",
            "beholder,tag=tagval heapUsedBytes=N",
            "beholder,tag=tagval messagesReceived=N",
            "beholder,tag=tagval packCount=N",
            "beholder,tag=tagval packDurationMaxNanos=N",
            "beholder,tag=tagval packDurationTotalNanos=N",
            "beholder,tag=tagval queueChunksCreated=N",
            "beholder,tag=tagval queueMaxSize=N",
            "beholder,tag=tagval queueOverflows=N",
            "beholder,tag=tagval unpackCount=N",
            "beholder,tag=tagval unpackDurationMaxNanos=N",
            "beholder,tag=tagval unpackDurationTotalNanos=N",
            "beholder,tag=tagval unparsedDropped=N",
            "beholder,tag=tagval uptimeSeconds=N"
        ), lineProtocolPackets)
    }
}
