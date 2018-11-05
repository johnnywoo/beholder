package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.Beholder
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.stats.Stats
import java.nio.channels.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

private val number = AtomicInteger()

class SelectorThread(private val app: Beholder) : Thread("selector${number.incrementAndGet()}") {
    private val isRunning = AtomicBoolean(true)

    init {
        isDaemon = true
    }

    private val selector = Selector.open()
    private val channels = mutableSetOf<ServerSocketChannel>()

    // Synchronization model of Selector makes register() and select() depend on each other.
    // We need to work around that.
    private val operationQueue = LinkedBlockingQueue<() -> Unit>()

    private fun runOperation(block: () -> Unit) {
        operationQueue.offer(block)
        selector.wakeup()
    }

    fun erase() {
        selector.close()
    }

    fun addTcpListener(block: Callback) {
        runOperation {
            val channel = ServerSocketChannel.open()
            channel.bind(block.getAddress().toSocketAddress())
            channel.configureBlocking(false)
            channel.register(selector, SelectionKey.OP_ACCEPT, block)
            channels.add(channel)
        }
    }

    fun removeTcpListener(address: Address) {
        runOperation {
            for (key in selector.keys()) {
                val attachment = key.attachment()
                if (attachment is Callback && attachment.getAddress() == address) {
                    try {
                        key.cancel()
                        channels.remove(key.channel())
                        key.channel().close()
                    } catch (e: Throwable) {
                        InternalLog.exception(e)
                    }
                }
            }
        }
    }

    override fun run() {
        try {
            while (isRunning.get()) {
                selector.select()

                while (true) {
                    val operation = operationQueue.poll()
                    if (operation == null) {
                        break
                    }
                    operation()
                }

                val selectedKeys = selector.selectedKeys()
                for (key in selectedKeys) {
                    val channel = key.channel()
                    if (channel == null) {
                        InternalLog.err("Null channel from selector")
                        continue
                    }

                    if (!key.isValid) {
                        continue
                    }

                    if (key.isAcceptable) {
                        // new connection
                        if (channel !is ServerSocketChannel) {
                            InternalLog.err("Weird channel got incoming connection: ${key.channel().javaClass.canonicalName}")
                            continue
                        }

                        val client = channel.accept()
                        if (client != null) {
                            client.configureBlocking(false)
                            client.register(selector, SelectionKey.OP_READ, key.attachment())

                            app.executor.execute {
                                Stats.reportTcpConnected()
                            }
                        }
                    }

                    if (key.isReadable) {
                        // new data
                        if (channel !is SocketChannel) {
                            InternalLog.err("Weird channel got incoming data: ${key.channel().javaClass.canonicalName}")
                            continue
                        }
                        val callback = key.attachment() as? Callback
                        if (callback == null) {
                            InternalLog.err("Weird channel with no callback got incoming data: ${key.channel().javaClass.canonicalName}")
                            continue
                        }

                        // пока мы там что-то читаем из канала, селектору надо сказать,
                        // чтобы он перестал слушать этот канал
                        key.interestOps(0)
                        app.executor.execute {
                            callback.processSocketChannel(channel)
                            if (key.isValid) {
                                key.interestOps(SelectionKey.OP_READ)
                            }
                            selector.wakeup()
                        }
                    }
                }
                selectedKeys.clear()
            }
        } catch (e: ClosedSelectorException) {
            InternalLog.info("Closed selector: ${e.javaClass.canonicalName} ${e.message}")

            isRunning.set(false)

            while (!operationQueue.isEmpty()) {
                operationQueue.poll()
            }

            for (channel in channels) {
                channel.close()
            }
        }
    }

    interface Callback {
        fun getAddress(): Address
        fun processSocketChannel(channel: SocketChannel)
    }
}

