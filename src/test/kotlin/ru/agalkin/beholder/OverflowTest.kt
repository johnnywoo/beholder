package ru.agalkin.beholder

import org.junit.jupiter.api.Disabled
import ru.agalkin.beholder.testutils.NetworkedTestAbstract
import java.net.*
import kotlin.test.Test
import kotlin.test.assertEquals

class OverflowTest : NetworkedTestAbstract() {
    @Test
    fun testSendAndReceive() {
        val config = "buffer { memory_compression off; } queue_chunk_messages 5; from udp 3821; to tcp 1212"

        val numberOfMessages = 20

        makeApp(config).use { app ->
            val root = app.config.root

            // Запускаем систему, ждём инициализации
            root.start()
            Thread.sleep(100)

            // Отправляем 20 сообщений, пока что TCP не сможет никуда их отправить
            repeat(20) {
                sendToUdp(3821, "cat")
            }

            // Теперь поднимаем TCP-сервер и принимаем накопленные в очереди сообщения
            val payloads = receivePayloadsByTcpServer(1212)
            assertEquals(numberOfMessages, payloads.size, "Expected number of messages does not match")
        }
    }

    @Test @Disabled("We don't know how to do TCP tests properly yet")
    fun testSendAndOverflow() {
        val messageText = "cat"

        val config = "buffer { memory_compression off; memory_bytes 5000; } queue_chunk_messages 5; from tcp 1211; to tcp 1212"

        val numberOfMessagesSent = 1010
        val numberOfMessagesReceived = 1010

        makeApp(config).use { app ->
            val root = app.config.root

            // Запускаем систему, ждём инициализации
            root.start()
            Thread.sleep(100)

            // Отправляем 20 сообщений, пока что TCP не сможет никуда их отправить
            val payloadsToSend = mutableListOf<String>()
            repeat(numberOfMessagesSent) {
                payloadsToSend.add(messageText+"\n")
            }
            sendPayloadsByTcpClient(1211, payloadsToSend)

            assertEquals(2650, app.defaultBuffer.currentSizeInMemory.get(), "Expected buffer size does not match")

            // Теперь поднимаем TCP-сервер и принимаем накопленные в очереди сообщения
            val payloads = receivePayloadsByTcpServer(1212)
            assertEquals(numberOfMessagesReceived, payloads.size, "Expected number of messages does not match")
        }
    }

    private fun sendPayloadsByTcpClient(port: Int, payloads: List<String>) {
        Socket().use {
            println("test socket client on port $port: created")
            it.connect(InetSocketAddress(InetAddress.getLocalHost(), port))
            println("test socket client on port $port: connected")
            for (payload in payloads) {
                it.getOutputStream().write(payload.toByteArray())
            }
            Thread.sleep(100)
            println("test socket client on port $port: closing")
        }
    }

    private fun receivePayloadsByTcpServer(port: Int): List<String> {
        val list = mutableListOf<String>()
        ServerSocket(port).use { server ->
            println("test socket server on port $port: started")
            try {
                server.accept().use { connection ->
                    println("test socket server on port $port: connected")
                    connection.soTimeout = 100 // millis
                    connection.getInputStream().reader().forEachLine {
                        list.add(it)
                    }
                }
            } catch (ignored: SocketTimeoutException) {
            }
        }
        return list
    }
}
