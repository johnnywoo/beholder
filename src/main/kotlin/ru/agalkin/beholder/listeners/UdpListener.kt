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
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


class UdpListener(private val address: Address) {
    private val queue = LinkedBlockingQueue<Message>()

    private var isEmitterPaused   by ThreadSafeFlag(false)
    private var isListenerDeleted by ThreadSafeFlag(false)

    private val listenerThread = object : Thread("from-udp-$address-listener") {
        private val buffer = ByteArray(MAX_MESSAGE_CHARS)

        override fun run() {
            println("Thread $name got started")

            DatagramSocket(address.port, address.getInetAddress()).use {socket ->
                socket.soTimeout = 100 // millis

                // лисенер останавливается, когда подписчики кончились
                while (!isListenerDeleted) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
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
            }

            println("Thread $name got deleted")
        }

        private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'") // Quoted "Z" to indicate UTC, no timezone offset
        init {
            formatter.timeZone = TimeZone.getTimeZone("UTC")
        }

        private fun curDateIso(): String
            = formatter.format(Date())
    }

    private val emitterThread = object : Thread("from-udp-$address-emitter") {
        override fun run() {
            println("Thread $name got started")

            // эмиттер останавливается, когда подписчики кончились
            while (!isListenerDeleted) {
                if (isEmitterPaused) {
                    Thread.sleep(50)
                    continue
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
        Beholder.receivers.add(object : Beholder.ReloadListener {
            override fun before() {
                // перед тем, как заменять конфиг приложения,
                // мы хотим поставить приём сообщений на паузу
                isEmitterPaused = true
            }

            override fun after() {
                if (receivers.isEmpty()) {
                   // после перезагрузки конфига оказалось, что листенер никому больше не нужен
                    isListenerDeleted = true
                    Beholder.receivers.remove(this)
                }
                isEmitterPaused = false
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

    private class ThreadSafeFlag(initialValue: Boolean) : ReadWriteProperty<Any, Boolean> {
        private val ab = AtomicBoolean(initialValue)

        override fun getValue(thisRef: Any, property: KProperty<*>): Boolean
            = ab.get()

        override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean)
            = ab.set(value)
    }
}
