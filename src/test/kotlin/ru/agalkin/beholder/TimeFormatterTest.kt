package ru.agalkin.beholder

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import ru.agalkin.beholder.testutils.TestAbstract
import ru.agalkin.beholder.testutils.TestInputProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TimeFormatterTest : TestAbstract() {
    private class TimeFormatProvider : TestInputProvider() {
        init {
            // predefined date formats
            case("date as time", "2018-06-12T11:26:12+01:00", "11:26:12")
            case("date as date", "2018-06-12T11:26:12+01:00", "2018-06-12")
            case("date as datetime", "2018-06-12T11:26:12+01:00", "2018-06-12T11:26:12+01:00")

            // GMT
            case("date as datetime", "2018-06-12T11:26:12+00:00", "2018-06-12T11:26:12+00:00")
            case("date as datetime", "2018-06-12T11:26:12Z", "2018-06-12T11:26:12+00:00")

            // unixtime
            case("date as unixtime-seconds", "2018-06-12T11:26:12+01:00", "1528799172")
            case("date as unixtime-milliseconds", "2018-06-12T11:26:12+01:00", "1528799172000")
            case("date as unixtime-microseconds", "2018-06-12T11:26:12+01:00", "1528799172000000")
            case("date as unixtime-nanoseconds", "2018-06-12T11:26:12+01:00", "1528799172000000000")

            // default formats
            case("time", "2018-06-12T11:26:12+01:00", "11:26:12")
            case("date", "2018-06-12T11:26:12+01:00", "2018-06-12")
        }
    }

    @ParameterizedTest
    @ArgumentsSource(TimeFormatProvider::class)
    fun testFormat(format: String, time: String, result: String) {
        val message = Message(mapOf("date" to FieldValue.fromString(time)))
        val processedMessage = processMessageWithConfig(message, "set \$d $format in \$date")
        assertNotNull(processedMessage)
        assertFieldNames(processedMessage, "d", "date")
        assertEquals(result, processedMessage?.getStringField("d"), "Definiftion '$format' input '$time'")
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
