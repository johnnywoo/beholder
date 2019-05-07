package ru.agalkin.beholder

import ru.agalkin.beholder.testutils.TestAbstract
import ru.agalkin.beholder.testutils.assertFieldNames
import ru.agalkin.beholder.testutils.assertFieldValues
import kotlin.test.Test
import kotlin.test.assertEquals

class EachFieldAsMessageInflaterTest : TestAbstract() {
    @Test
    fun testEachFieldAsMessageInflater() {
        val received = receiveMessagesWithConfig("parse each-field-as-message", 2) {
            val message = Message.of(
                "cat" to "feline",
                "dog" to "canine"
            )
            it.topLevelInput.addMessage(message)
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
            { it.topLevelInput.addMessage(Message()) }
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
            "payload", "influxLineProtocolPayload"
        )
    }

    @Test
    fun testStatsLineProtocolNoTags() {
        val received = receiveMessagesWithConfig(
            "parse beholder-stats",
            1,
            { it.topLevelInput.addMessage(Message()) }
        )

        // beholder uptimeSeconds=0,heapBytes=268435456,heapUsedBytes=20428360,heapMaxBytes=4294967296,defaultBufferAllocatedBytes=0,unpackDurationMaxNanos=0,allBuffersAllocatedBytes=0,compressBeforeTotalBytes=0,messagesReceived=0,fromTcpMessages=0,fromTcpTotalBytes=0,packDurationTotalNanos=0,fromUdpMessages=0,decompressDurationTotalNanos=0,decompressCount=0,unpackCount=0,compressAfterTotalBytes=0,packCount=0,defaultBufferMaxBytes=0,compressDurationMaxNanos=0,queueMaxSize=0,fromTcpMaxBytes=0,fromUdpTotalBytes=0,decompressDurationMaxNanos=0,packDurationMaxNanos=0,queueOverflows=0,compressCount=0,compressDurationTotalNanos=0,allBuffersMaxBytes=0,fromUdpMaxBytes=0,unparsedDropped=0,unpackDurationTotalNanos=0,configReloads=0,fromTcpNewConnections=0,queueChunksCreated=0 1536478714733216000
        assertEquals(
            "beholder field-values-and-nanos",
            received[0].getStringField("influxLineProtocolPayload").replace("([a-zA-Z]+=\\d+,)*[a-zA-Z]+=\\d+ \\d{19}$".toRegex(), "field-values-and-nanos")
        )
    }

    @Test
    fun testStatsLineProtocolTags() {
        val received = receiveMessagesWithConfig(
            "set \$host somehost; set \$animal cat; parse beholder-stats",
            1,
            { it.topLevelInput.addMessage(Message()) }
        )

        assertEquals(
            "beholder,animal=cat,host=somehost field-values-and-nanos",
            received[0].getStringField("influxLineProtocolPayload").replace("([a-zA-Z]+=\\d+,)*[a-zA-Z]+=\\d+ \\d{19}$".toRegex(), "field-values-and-nanos")
        )
    }

    @Test
    fun testStatsInfluxizer() {
        val received = receiveMessagesWithConfig(
            """
                parse beholder-stats;
                parse each-field-as-message;

                switch ¥value {
                    case ~^[0-9]+¥~ {}
                    default {drop}
                }
                set ¥payload 'beholder,tag=tagval ¥key=¥value';
            """.replace('¥', '$'),
            35,
            {
                val message = Message.of(
                    "date" to "2017-11-26T16:16:01+03:00",
                    "payload" to "lots of irrelevant words"
                )
                it.topLevelInput.addMessage(message)
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
