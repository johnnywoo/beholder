package ru.agalkin.beholder

import org.junit.Test
import kotlin.test.assertEquals

class DataBufferTest : TestAbstract() {
    @Test
    fun testSmoke() {
        val messageText = "<15>1 2017-03-03T09:26:44+00:00 sender-host program-name 12345 - - Message: поехали!"

        val config = "queue_chunk_messages 5; from udp 3821; to tcp 1212"

        receiveMessagesWithConfig(config, 20) { _ ->
            repeat(20) {
                sendToUdp(3821, messageText)
            }
        }
    }

    @Test
    fun testNowhereToSend() {
        val messageText = "Message: поехали!"
        val config = "buffer_compression off; queue_chunk_messages 5; from udp 3821; to tcp 1212"

        makeApp(config).use { app ->
            val root = app.config.root

            var processedMessagesNum = 0
            val sentMessagesNum = 20

            root.topLevelOutput.addStep(conveyorStepOf {
                processedMessagesNum++
            })

            root.start()
            Thread.sleep(100)

            repeat(sentMessagesNum) {
                sendToUdp(3821, messageText)
            }

            var timeSpentMillis = 0
            while (timeSpentMillis < 300) {
                if (processedMessagesNum == sentMessagesNum) {
                    break
                }
                Thread.sleep(50)
                timeSpentMillis += 50
            }

            assertEquals(sentMessagesNum, processedMessagesNum, "Expected number of messages does not match")

            // total buffer length should be:
            // 20 messages / chunk length 5 = 4 chunks
            // we ignore first and last chunk = 2 chunks in buffer
            // "Message: поехали!" = 24 bytes (17 length + 7 cyrillic second bytes)
            // plus \n from tcp newline-terminated = +1 byte
            // plus varint length = +1 byte
            // 26 bytes per message * 10 messages = 260 bytes

            // this is of course not the actual memory usage, but it's something we can measure properly

            assertEquals(260, app.defaultBuffer.currentSizeInMemory.get())
        }
    }

    @Test
    fun testNowhereToSendCompressed() {
        val messageText = "Message: поехали!"
        val config = "queue_chunk_messages 5; from udp 3821; to tcp 1212"

        makeApp(config).use { app ->
            val root = app.config.root

            var processedMessagesNum = 0
            val sentMessagesNum = 20

            root.topLevelOutput.addStep(conveyorStepOf {
                processedMessagesNum++
            })

            root.start()
            Thread.sleep(100)

            repeat(sentMessagesNum) {
                sendToUdp(3821, messageText)
            }

            var timeSpentMillis = 0
            while (timeSpentMillis < 300) {
                if (processedMessagesNum == sentMessagesNum) {
                    break
                }
                Thread.sleep(50)
                timeSpentMillis += 50
            }

            assertEquals(sentMessagesNum, processedMessagesNum, "Expected number of messages does not match")

            // when uncompressed, total buffer length is 260 bytes
            // with default lz4-fast compression, it gets down to 74 bytes

            assertEquals(74, app.defaultBuffer.currentSizeInMemory.get())
        }
    }
}
