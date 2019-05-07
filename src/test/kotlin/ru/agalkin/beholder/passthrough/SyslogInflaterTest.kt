package ru.agalkin.beholder.passthrough

import ru.agalkin.beholder.Message
import ru.agalkin.beholder.formatters.DumpFormatter
import ru.agalkin.beholder.testutils.TestAbstract
import kotlin.test.Test
import kotlin.test.assertEquals

class SyslogInflaterTest : TestAbstract() {
    @Test
    fun testSyslogInflater() {
        val message = Message.of(
            "payload" to "<190>Nov 25 13:46:44 vps nginx: 127.0.0.1 - - [25/Nov/2017:13:46:44 +0300] \"GET /api HTTP/1.1\" 200 47 \"-\" \"curl/7.38.0\""
        )

        val processedMessage = processMessageWithConfig(message, "parse syslog")

        assertEquals(
            """
            |¥facility=23
            |¥host=vps
            |¥payload=127.0.0.1 - - [25/Nov/2017:13:46:44 +0300] "GET /api HTTP/1.1" 200 47 "-" "curl/7.38.0"
            |¥program=nginx
            |¥severity=6
            """.trimMargin().replace('¥', '$'),
            DumpFormatter().formatMessage(processedMessage!!).toString().replace(Regex("^.*\n"), "")
        )
    }

    @Test
    fun testSyslogInflaterMultiline() {
        val message = Message.of(
            "payload" to "<190>Nov 25 13:46:44 vps nginx: a\nb"
        )

        val processedMessage = processMessageWithConfig(message, "parse syslog")

        assertEquals("a\nb", processedMessage!!.getPayloadString())
    }

    @Test
    fun testSyslogInflaterIetfNoMillis() {
        val message = Message.of(
            "payload" to "<15>1 2017-03-03T09:26:44+00:00 sender-host program-name 12345 - - Message: поехали!"
        )

        val processedMessage = processMessageWithConfig(message, "parse syslog")

        assertEquals("Message: поехали!", processedMessage!!.getPayloadString())
    }

    @Test
    fun testSyslogInflaterIetfWithMillis() {
        val message = Message.of(
            "payload" to "<15>1 2017-03-03T09:26:44.999+00:00 sender-host program-name 12345 - - Message: поехали!"
        )

        val processedMessage = processMessageWithConfig(message, "parse syslog")

        assertEquals("Message: поехали!", processedMessage!!.getPayloadString())
    }
}
