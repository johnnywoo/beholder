package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.Address
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class UdpListener(private val address: Address) {
    val queue = LinkedBlockingQueue<Message>()

    val isEmitterPaused   = AtomicBoolean(false)
    val isListenerDeleted = AtomicBoolean(false)

    private val listenerThread = object : Thread("from-udp-$address-listener") {
        override fun run() {
            println("Thread $name got started")

            val datagramSocket = DatagramSocket(address.port, address.getInetAddress())
            datagramSocket.soTimeout = 100 // millis

            val buffer = ByteArray(MAX_MESSAGE_CHARS)

            // лисенер останавливается, когда подписчики кончились
            while (!isListenerDeleted.get()) {
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    datagramSocket.receive(packet)
                } catch (e: SocketTimeoutException) {
                    // 100 millis passed without any packet
                    // just loop around, check proper conditions and then receive again
                    continue
                }

                // не даём очереди бесконтрольно расти (вытесняем старые записи)
                if (queue.size > MAX_BUFFER_COUNT) {
                    queue.take()
                }

                val message = Message(String(packet.data, 0, packet.length))

                message.tags["receivedDate"] = curDateIso()
                message.tags["fromHost"]     = packet.address.hostAddress
                message.tags["fromPort"]     = packet.port.toString()
                message.tags["toHost"]       = address.getInetAddress().hostAddress
                message.tags["toPort"]       = address.port.toString()

                queue.offer(message)
            }

            println("Thread $name got deleted")
        }
    }

    private fun curDateIso(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'") // Quoted "Z" to indicate UTC, no timezone offset
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date())
    }

    private val emitterThread = object : Thread("from-udp-$address-emitter") {
        override fun run() {
            println("Thread $name got started")

            // эмиттер останавливается, когда подписчики кончились
            while (!isListenerDeleted.get()) {
                while (isEmitterPaused.get()) {
                    Thread.sleep(50)
                }
                val message = queue.poll(100, TimeUnit.MILLISECONDS) // blocking for 100 millis
                if (message == null) {
                    // за 100 мс ничего не нашли
                    // проверим все условия и поедем ждать заново
                    continue
                }
                for (receiver in receivers) {
                    receiver(message)
                }
            }

            // на всякий случай, если мы будем перезапускать лисенер, надо тут всё зачистить
            queue.clear()

            println("Thread $name got deleted")
        }
    }

    init {
        Beholder.addReceiver(object : Beholder.ReloadListener {
            override fun before() {
                // перед тем, как заменять конфиг приложения,
                // мы хотим поставить приём сообщений на паузу
                isEmitterPaused.set(true)
                isListenerDeleted.set(false)
            }

            override fun after() {
                if (receivers.isEmpty()) {
                   // после перезагрузки конфига оказалось, что листенер никому больше не нужен
                    isListenerDeleted.set(true)
                }
                isEmitterPaused.set(false)
            }
        })

        listenerThread.start()
        emitterThread.start()
    }

    val receivers = mutableSetOf<(Message) -> Unit>()

    companion object {
        val MAX_BUFFER_COUNT  = 1000 // messages
        val MAX_MESSAGE_CHARS = 10 * 1024 * 1024

        private val listeners = WeakHashMap<Address, UdpListener>()

        fun getListener(address: Address): UdpListener {
            synchronized(listeners) {
                if (!listeners.contains(address)) {
                    listeners[address] = UdpListener(address)
                }
                return listeners[address]!!
            }
        }
    }
}
