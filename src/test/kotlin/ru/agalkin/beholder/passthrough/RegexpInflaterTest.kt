package ru.agalkin.beholder.passthrough

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.testutils.TestAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RegexpInflaterTest : TestAbstract() {
    @Test
    fun testRegexpInflater() {
        val message = Message.of("payload" to "We've got cats and dogs")

        val parsedMessage = processMessageWithConfig(message, "parse ~(?<animal>cat|dog)~")

        assertEquals(
            """
            |¥animal=cat
            |¥payload=We've got cats and dogs
            """.trimMargin(),
            getMessageDump(parsedMessage)
        )
    }

    @Test
    fun testRegexpInflaterNoMatch() {
        val message = Message.of("payload" to "We've got cats and dogs")

        val parsedMessage = processMessageWithConfig(message, "parse ~(?<animal>whale)~")

        assertNull(parsedMessage)
    }

    @Test
    fun testRegexpInflaterKeepUnparsed() {
        val message = Message.of("payload" to "We've got cats and dogs")

        val parsedMessage = processMessageWithConfig(message, "parse keep-unparsed ~(?<animal>whale)~")

        assertEquals(
            "¥payload=We've got cats and dogs",
            getMessageDump(parsedMessage)
        )
    }

    @Test
    fun testRegexpInflaterNumberedGroup() {
        val message = Message.of("payload" to "We've got cats and dogs")

        val parsedMessage = processMessageWithConfig(message, "parse ~(cat)~")

        // there are no named groups, so nothing should change
        assertEquals(
            "¥payload=We've got cats and dogs",
            getMessageDump(parsedMessage)
        )
    }

    @Test
    fun testRegexpInflaterNoMatchNoOverwrite() {
        val message = Message.of(
            "payload" to "We've got cats and dogs",
            "animal" to "headcrab"
        )

        val parsedMessage = processMessageWithConfig(message, "parse keep-unparsed ~(?<animal>whale)~")

        assertEquals(
            """
            |¥animal=headcrab
            |¥payload=We've got cats and dogs
            """.trimMargin(),
            getMessageDump(parsedMessage)
        )
    }
}
