package ru.agalkin.beholder

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

const val AGENT_UDP_SYSLOG_PORT = 10002
const val COLLECTOR_TCP_PORT    = 11000

class IntegrationTest : TestAbstract() {
    private val agentConfig = """
        from_udp_buffer_messages_count 10000;
        to_tcp_buffer_messages_count 10000;

        # Access log from nginx
        # access_log syslog:server=172.17.0.1:10000,tag=robot_dvru;
        join { from udp 10000; set ¥agent_type 'udp-nginx-access'; }

        # Error log from nginx
        # error_log syslog:server=172.17.0.1:10001,tag=robot_dvru;
        join { from udp 10001; set ¥agent_type 'udp-nginx-error'; }

        # UDP syslog
        join { from udp 10002; set ¥agent_type 'udp-syslog'; }

        # TCP syslog newline-terminated
        join { from tcp 10100; set ¥agent_type 'tcp-nl-syslog'; }

        # TCP JSON newline-terminated
        join { from tcp 10101; set ¥agent_type 'tcp-nl-json'; }

        # TCP syslog length-space-data
        join { from tcp 10102 as syslog-frame; set ¥agent_type 'tcp-sf-syslog'; }

        # TCP JSON length-space-data
        join { from tcp 10103 as syslog-frame; set ¥agent_type 'tcp-sf-json'; }

        # Agent simply wraps everything into JSON and sends it to the collector.
        set ¥host 'fake-host';
        set ¥date '';
        set ¥from '';
        set ¥payload json;
    """.replace('¥', '$')

    private val collectorConfig = """
        from tcp 11000;
        parse json;

        set ¥agent_host ¥host;

        switch ¥agent_type {
            case ~-syslog¥~ {
                parse syslog;
            }
            case ~-json¥~ {
                parse json;
            }
            case ~^internal-log¥~ {
                set ¥program beholder-agent;
            }
        }

        set ¥pid replace ~^¥~ '-' in ¥pid;
        set ¥level severity-name ¥severity;
        # 2018-05-03T15:32:56+03:00 10.64.1.13 36439 INFO Message
        set ¥payload '¥date ¥agent_host ¥pid ¥level ¥payload';

        set ¥host basename ¥host;
        set ¥program basename ¥program;

        set ¥file '/logs/¥host/¥program.log';
    """.replace('¥', '$')



    @Test
    fun testAgentCyrillic() {
        val messageText = "<15>1 2017-03-03T09:26:44+00:00 sender-host program-name 12345 - - Message: поехали!"

        val processedMessage = receiveMessageWithConfig(agentConfig) {
            sendToUdp(AGENT_UDP_SYSLOG_PORT, messageText)
        }

        assertNotNull(processedMessage)
        if (processedMessage == null) {
            return
        }
        assertEquals(
            """{"agent_type":"udp-syslog","host":"fake-host","payload":"<15>1 2017-03-03T09:26:44+00:00 sender-host program-name 12345 - - Message: поехали!"}""",
            processedMessage.getPayloadString()
        )
    }

    @Test
    fun testCollectorCyrillic() {
        val messageText = """{"agent_type":"udp-syslog","host":"fake-host","payload":"<15>1 2017-03-03T09:26:44+00:00 sender-host program-name 12345 - - Message: поехали!"}"""

        val processedMessage = receiveMessageWithConfig(collectorConfig) {
            sendToTcp(COLLECTOR_TCP_PORT, (messageText + "\n").toByteArray(Charsets.UTF_8))
        }

        assertNotNull(processedMessage)
        if (processedMessage == null) {
            return
        }
        assertEquals(
            "2017-03-03T09:26:44+00:00 fake-host 12345 DEBUG Message: поехали!",
            processedMessage.getPayloadString()
        )
        assertEquals(
            "/logs/sender-host/program-name.log",
            processedMessage.getStringField("file")
        )
    }
}
