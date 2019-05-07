package ru.agalkin.beholder

import ru.agalkin.beholder.testutils.NetworkedTestAbstract
import ru.agalkin.beholder.testutils.assertFieldNames
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CreateDatesInTimezoneTest : NetworkedTestAbstract() {
    @Test
    fun testCreateDatesInTimezoneMoscow() {
        val messageText = "text"
        val processedMessage = feedMessagesIntoConfig("create_dates_in_timezone Europe/Moscow; from udp 3820") {
            sendToUdp(3820, messageText)
        }

        assertNotNull(processedMessage)
        assertFieldNames(processedMessage, "date", "from", "payload")
        val dateString = processedMessage.getStringField("date")
        assertEquals("+03:00", dateString.substring(dateString.length - 6))
    }

    @Test
    fun testCreateDatesInTimezoneUtc() {
        val messageText = "text"
        val processedMessage = feedMessagesIntoConfig("create_dates_in_timezone UTC; from udp 3820") {
            sendToUdp(3820, messageText)
        }

        assertNotNull(processedMessage)
        assertFieldNames(processedMessage, "date", "from", "payload")
        val dateString = processedMessage.getStringField("date")
        assertEquals("+00:00", dateString.substring(dateString.length - 6))
    }
}
