package ru.agalkin.beholder

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TimeFormatterTest : TestAbstract() {
    @Test
    fun testFormat() {
        val tests = mapOf(
            // predefined date formats
            "date as time" to Pair("2018-06-12T11:26:12+01:00", "11:26:12"),
            "date as date" to Pair("2018-06-12T11:26:12+01:00", "2018-06-12"),
            "date as datetime" to Pair("2018-06-12T11:26:12+01:00", "2018-06-12T11:26:12+01:00"),

            // GMT
            "date as datetime" to Pair("2018-06-12T11:26:12+00:00", "2018-06-12T11:26:12+00:00"),
            "date as datetime" to Pair("2018-06-12T11:26:12Z", "2018-06-12T11:26:12+00:00"),

            // unixtime
            "date as unixtime-seconds" to Pair("2018-06-12T11:26:12+01:00", "1528799172"),
            "date as unixtime-milliseconds" to Pair("2018-06-12T11:26:12+01:00", "1528799172000"),
            "date as unixtime-microseconds" to Pair("2018-06-12T11:26:12+01:00", "1528799172000000"),
            "date as unixtime-nanoseconds" to Pair("2018-06-12T11:26:12+01:00", "1528799172000000000"),

            // default formats
            "time" to Pair("2018-06-12T11:26:12+01:00", "11:26:12"),
            "date" to Pair("2018-06-12T11:26:12+01:00", "2018-06-12")
        )

        for ((formatDefiniton, pair) in tests) {
            val message = Message(mapOf("date" to FieldValue.fromString(pair.first)))
            val processedMessage = processMessageWithConfig(message, "set \$d $formatDefiniton in \$date")
            assertNotNull(processedMessage)
            assertFieldNames(processedMessage, "d", "date")
            assertEquals(pair.second, processedMessage?.getStringField("d"), "Definiftion '$formatDefiniton' input '${pair.first}'")
        }
    }

    @Test
    fun testCreateDatesInTimezoneMoscow() {
        val messageText = "text"
        val processedMessage = receiveMessageWithConfig("create_dates_in_timezone Europe/Moscow; from udp 3820") {
            sendToUdp(3820, messageText)
        }

        assertNotNull(processedMessage)
        if (processedMessage == null) {
            return
        }
        assertFieldNames(processedMessage, "date", "from", "payload")
        val dateString = processedMessage.getStringField("date")
        assertEquals("+03:00", dateString.substring(dateString.length - 6))
    }

    @Test
    fun testCreateDatesInTimezoneUtc() {
        val messageText = "text"
        val processedMessage = receiveMessageWithConfig("create_dates_in_timezone UTC; from udp 3820") {
            sendToUdp(3820, messageText)
        }

        assertNotNull(processedMessage)
        if (processedMessage == null) {
            return
        }
        assertFieldNames(processedMessage, "date", "from", "payload")
        val dateString = processedMessage.getStringField("date")
        assertEquals("+00:00", dateString.substring(dateString.length - 6))
    }

    @Test
    fun testCreateDatesInTimezoneInvalid() {
        assertConfigFails(
            "create_dates_in_timezone NoSuchTimezone",
            "Invalid timezone: NoSuchTimezone: create_dates_in_timezone NoSuchTimezone [test-config:1]"
        )
    }
}
