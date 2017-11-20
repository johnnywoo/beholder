package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.Message
import ru.agalkin.beholder.config.Address
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

class UdpListener(private val address: Address) {
    val queue = LinkedBlockingQueue<Message>()

    val isPaused = AtomicBoolean(false)

    private val listenerThread = object : Thread("from-udp-$address-listener") {
        override fun run() {
            val datagramSocket = DatagramSocket(address.port, address.getInetAddress())
            val buffer = ByteArray(MAX_MESSAGE_CHARS)

            while (true) {
                val packet = DatagramPacket(buffer, buffer.size)
                datagramSocket.receive(packet)
                val text = String(packet.data, 0, packet.length)

                // не даём очереди бесконтрольно расти (вытесняем старые записи)
                if (queue.size > MAX_BUFFER_COUNT) {
                    queue.take()
                }

                queue.offer(Message(text))
            }
        }
    }

    private val emitterThread = object : Thread("from-udp-$address-emitter") {
        override fun run() {
            while (true) {
                while (isPaused.get()) {
                    Thread.sleep(50)
                }
                val message = queue.take() // blocking
                for (receiver in receivers) {
                    receiver(message)
                }
            }
        }
    }

    init {
        Beholder.addReceiver(object : Beholder.ReloadListener {
            override fun before() {
                // перед тем, как заменять конфиг приложения,
                // мы хотим поставить приём сообщений на паузу
                isPaused.set(true)
            }

            override fun after() {
                // TODO после перезагрузки конфига оказалось, что листенер никому больше не нужен
                isPaused.set(false)
            }
        })

        println("Starting UDP listener on $address")
        listenerThread.start()
        println("Starting UDP listener emitter thread")
        emitterThread.start()
    }

    private val receivers = mutableSetOf<(Message) -> Unit>()

    fun addReceiver(receiver: (Message) -> Unit)
        = receivers.add(receiver)

    fun removeReceiver(receiver: (Message) -> Unit)
        = receivers.remove(receiver)

    companion object {
        val MAX_BUFFER_COUNT  = 1000 // messages
        val MAX_MESSAGE_CHARS = 10 * 1024 * 1024

        private val listeners = mutableMapOf<Address, UdpListener>()

        fun getListener(address: Address): UdpListener {
            if (!listeners.contains(address)) {
                listeners[address] = UdpListener(address)
            }
            return listeners[address]!!
        }
    }
}
