package ru.agalkin.beholder.listeners

import ru.agalkin.beholder.Executor
import ru.agalkin.beholder.InternalLog
import ru.agalkin.beholder.config.Address
import ru.agalkin.beholder.stats.Stats
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.LinkedBlockingQueue

object SelectorThread : Thread("selector") {
    init {
        isDaemon = true
        start()
    }

    private val selector = Selector.open()

    // Synchronization model of Selector makes register() and select() depend on each other.
    // We need to work around that.
    private val operationQueue = LinkedBlockingQueue<() -> Unit>()

    private fun runOperation(block: () -> Unit) {
        operationQueue.offer(block)
        selector.wakeup()
    }

    fun erase() {
        while (!operationQueue.isEmpty()) {
            operationQueue.poll()
        }
        runOperation {
            for (key in selector.keys()) {
                key.cancel()
                key.channel().close()
            }
        }
    }

    fun addTcpListener(address: Address, block: (SocketChannel) -> Unit) {
        runOperation {
            val channel = ServerSocketChannel.open()
            channel.bind(address.toSocketAddress())
            channel.configureBlocking(false)
            channel.register(selector, SelectionKey.OP_ACCEPT, Callback(address, block))
        }
    }

    fun removeTcpListener(address: Address) {
        runOperation {
            for (key in selector.keys()) {
                val attachment = key.attachment()
                if (attachment is Callback && attachment.address == address) {
                    try {
                        key.cancel()
                        key.channel().close()
                    } catch (e: Throwable) {
                        InternalLog.exception(e)
                    }
                }
            }
        }
    }

    override fun run() {
        while (true) {
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

                        Executor.execute {
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
                    Executor.execute {
                        callback.run(channel)
                        key.interestOps(SelectionKey.OP_READ)
                        selector.wakeup()
                    }
                }
            }
            selectedKeys.clear()
        }
    }

    private class Callback(val address: Address, private val block: (SocketChannel) -> Unit) {
        fun run(channel: SocketChannel)
            = block(channel)
    }
}

