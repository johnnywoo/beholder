package ru.agalkin.beholder

import org.junit.Test
import ru.agalkin.beholder.formatters.DumpFormatter
import kotlin.test.assertEquals

class SyslogInflaterTest : TestAbstract() {
    @Test
    fun testSyslogInflater() {
        val message = Message()
        message["payload"] = "<190>Nov 25 13:46:44 vps nginx: 127.0.0.1 - - [25/Nov/2017:13:46:44 +0300] \"GET /api HTTP/1.1\" 200 47 \"-\" \"curl/7.38.0\""

        val processedMessage = processMessageWithCommand(message, "parse syslog")

        assertEquals(
            """
                |¥payload=127.0.0.1 - - [25/Nov/2017:13:46:44 +0300] "GET /api HTTP/1.1" 200 47 "-" "curl/7.38.0"
                |¥syslogFacility=23
                |¥syslogSeverity=6
                |¥syslogHost=vps
                |¥syslogProgram=nginx
                """.trimMargin().replace('¥', '$'),
            DumpFormatter().formatMessage(processedMessage!!).replace(Regex("^.*\n"), "")
        )
    }

    @Test
    fun testSyslogInflaterMultiline() {
        val message = Message()
        message["payload"] = "<190>Nov 25 13:46:44 vps nginx: a\nb"

        val processedMessage = processMessageWithCommand(message, "parse syslog")

        assertEquals("a\nb", processedMessage!!.getPayload())
    }
}
