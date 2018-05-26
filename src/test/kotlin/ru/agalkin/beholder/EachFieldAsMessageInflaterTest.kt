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
            "queueMaxSize", "queueOverflows", "uptimeSeconds", "messagesReceived", "configReloads",
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
            15,
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
            "beholder,tag=tagval configReloads=N",
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
            "beholder,tag=tagval queueMaxSize=N",
            "beholder,tag=tagval queueOverflows=N",
            "beholder,tag=tagval uptimeSeconds=N"
        ), lineProtocolPackets)
    }
}
