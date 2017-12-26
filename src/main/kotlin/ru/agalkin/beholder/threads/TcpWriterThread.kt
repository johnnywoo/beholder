package ru.agalkin.beholder.threads

import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.getClosestFromRange
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.min

class TcpWriterThread(private val address: Address) : Thread("tcp-writer-$address") {
    val isWriterPaused = AtomicBoolean(false)
    val isWriterDestroyed = AtomicBoolean(false)

    val queue = LinkedBlockingQueue<String>()

    val reconnectIntervalSeconds = AtomicInteger()

    override fun run() {
        InternalLog.info("Thread $name $id got started")

        while (!isWriterDestroyed.get()) {
            try {
                connectAndLoop()
            } catch (e: Throwable) {
                InternalLog.err("$name ${e::class.simpleName} ${e.message}")
            }
        }

        // соединения нет, коннект ушёл из конфига
        // просто больше ничего не делаем (что там было в очереди, всё идёт на фиг)

        InternalLog.info("Thread $name $id got stopped")
    }

    private fun connect(socket: Socket): Boolean {
        try {
            val sleepChunkMillis = 50L
            var toSleepMillis = reconnectIntervalSeconds.get() * 1000L
            while (toSleepMillis > 0) {
                sleep(sleepChunkMillis)
                // если вдруг reconnectIntervalSeconds изменился в процессе ожидания (например, по SIGHUP),
                // значит надо перестать ждать
                toSleepMillis = min(toSleepMillis - sleepChunkMillis, reconnectIntervalSeconds.get() * 1000L)
            }

            if (isWriterDestroyed.get()) {
                // соединения нет, коннект ушёл из конфига
                return false
            }

            InternalLog.info("Attempting connection to TCP $address")
            socket.connect(InetSocketAddress(address.getInetAddress(), address.port), TO_TCP_CONNECT_TIMEOUT_MILLIS)

            reconnectIntervalSeconds.set(0)

            InternalLog.info("Connected to TCP $address")

            return true
        } finally {
            if (!socket.isConnected) {
                // socket was not connected
                // increase the waiting interval
                val newIntervalSeconds = getClosestFromRange(1..60, reconnectIntervalSeconds.get() * 2)
                reconnectIntervalSeconds.set(newIntervalSeconds)
                InternalLog.info("Will reconnect to TCP $address after $newIntervalSeconds seconds")
            }
        }
    }

    private var undeliveredText: String? = null

    private fun connectAndLoop() {
        Socket().use { socket ->
            if (!connect(socket)) {
                // пока мы ждали reconnect, сокет стал не нужен (ушёл из конфига)
                return
            }

            socket.keepAlive = true

            // ignore any input from the connection
            ignoreSocketInput(socket)

            val outputStream = socket.getOutputStream()
            while (true) {
                sleepWhilePaused()

                val text = undeliveredText ?: queue.poll(100, TimeUnit.MILLISECONDS) // blocking for 100 millis
                if (text == null) {
                    if (isWriterDestroyed.get()) {
                        // очередь закончилась и коннект больше не нужен
                        break // ends connectAndLoop()
                    }
                    // за 100 мс ничего не нашли
                    // проверим все условия и поедем ждать заново
                    continue
                }

                // если в процессе отправки сообщения будет проблема (исключение),
                // то в следующий раз надо не брать сообщение из очереди,
                // а пытаться отправить то же самое второй раз
                undeliveredText = text

                sleepWhilePaused()

                outputStream.write(text.toByteArray())
                outputStream.flush()

                undeliveredText = null
            }
        }
    }

    private fun ignoreSocketInput(socket: Socket) {
        // ignore any input from the connection
        thread(isDaemon = true, name = "$name-skipper") {
            val devNull = ByteArray(1024)
            val inputStream = socket.getInputStream()
            while (true) {
                try {
                    // тут возможны два варианта
                    // 1. read() будет ждать чего-то читать в блокирующем режиме
                    // 2. read() почует, что там всё кончилось (end of file is detected) и начнёт отдавать -1 без задержки
                    if (inputStream.read(devNull) < 0) {
                        break
                    }
                    InternalLog.info("Wrapping inputStream.read(devNull)")
                } catch (ignored: SocketException) {
                    InternalLog.info("inputStream.read(devNull) made exception ${ignored::class.simpleName} ${ignored.message}")
                }
            }
        }
    }

    private fun sleepWhilePaused() {
        while (isWriterPaused.get()) {
            Thread.sleep(50)
        }
    }
}