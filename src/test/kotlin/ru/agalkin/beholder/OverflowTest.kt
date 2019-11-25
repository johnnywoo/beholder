package ru.agalkin.beholder

import ru.agalkin.beholder.compressors.NoCompressor
import ru.agalkin.beholder.queue.DataBuffer
import ru.agalkin.beholder.queue.FieldValueQueue
import ru.agalkin.beholder.queue.Received
import ru.agalkin.beholder.testutils.NetworkedTestAbstract
import java.net.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.*

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

    @Test
    @Ignore
    fun testQueueOverflow() {
        makeApp("buffer { memory_compression off; memory_bytes 1000; } queue_chunk_messages 5;").use { app ->
            assertFalse(app.defaultBuffer.compressor is NoCompressor)
            app.config.root.start()
            assertTrue(app.defaultBuffer.compressor is NoCompressor)

            val shouldReceive = AtomicBoolean(false)
            val received = mutableListOf<FieldValue>()

            val queue = FieldValueQueue(app) { fieldValue ->
                if (shouldReceive.get()) {
                    received.add(fieldValue)
                    return@FieldValueQueue Received.OK
                } else {
                    Thread.sleep(10)
                    return@FieldValueQueue Received.RETRY
                }
            }

            // Пихаем в очередь 100 значений. Они все не влезут.
            repeat(100) {
                // Пихаем 99 байт данных + 1 байт на длину. В буфер 1000 байт должны влезть только 10 значений.
                queue.add(FieldValue.fromString(String.format("%04d", it) + "~".repeat(95)))
            }

            // 10 значений влезло, по 5 на кусок, получаем 2 куска в буфере.
            val byteArrays = app.defaultBuffer.getByteArraysOnlyForTests()
            assertEquals(2, byteArrays.size)

            // Уничтожаем лишние byte arrays в буфере, некоторые weak ref остаются пустыми.
            Runtime.getRuntime().gc()

//            // Пихали 100 сообщений, по 5 на чанк, получаем 20 чанков.
//            // Поскольку мы больше добавлять чанки не будем, очистка их не произойдёт и дохлые чанки не удалятся.
//            val chunks = queue.getChunksOnlyForTests()
//            assertEquals(20, chunks.size)
//
//            for (chunkNumber in chunks.indices) {
//                val chunk = chunks[chunkNumber]
//                when (chunkNumber) {
//                    0, 19 -> {
//                        assertEquals("not buffered", chunk.getBufferedStateOnlyForTests(), "Invalid state for chunk $chunkNumber")
//                    }
//                    in 1..16 -> {
//                        // Данные умерли в буфере
//                        assertEquals("buffered", chunk.getBufferedStateOnlyForTests(), "Invalid state for chunk $chunkNumber")
//                        assertNull(chunk.getByteArrayReferenceOnlyForTests().get(), "Invalid weak ref for chunk $chunkNumber")
//                    }
//                    else -> {
//                        // Данные выжили в буфере
//                        assertEquals("buffered", chunk.getBufferedStateOnlyForTests(), "Invalid state for chunk $chunkNumber")
//                        assertNotNull(chunk.getByteArrayReferenceOnlyForTests().get(), "Invalid weak ref for chunk $chunkNumber")
//                    }
//                }
//            }

            // Включаем режим приёма сообщений и ждём, пока они приедут.
            shouldReceive.set(true)
            Thread.sleep(100)

            // Сейчас выживают следующие записи:
            // 0..4 первый чанк не попадает в буфер, его начинают сразу же читать, поэтому он выживает
            // 85..94 данные, которые выжили в буфере
            // 95..99 последний чанк не попадает в буфер (система надеется, что мы читаем быстрее, чем пишем, и тогда ничего не поедет в буфер вообще — оптимизация)
            val dump = received.joinToString("\n")
            assertEquals(
                """
                    0000~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    0001~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    0002~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    0003~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    0004~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    0085~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    0086~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    0087~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    0088~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    0089~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    0090~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    0091~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    0092~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    0093~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    0094~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    0095~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    0096~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    0097~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    0098~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    0099~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                """.trimIndent(),
                dump,
                "Unexpected messages survived in the queue"
            )
        }
    }

//    @Test
//    fun testQueueOverflowChunkCleanup() {
//        makeApp("buffer { memory_compression off; memory_bytes 1000; } queue_chunk_messages 5;").use { app ->
//            assertFalse(app.defaultBuffer.compressor is NoCompressor)
//            app.config.root.start()
//            assertTrue(app.defaultBuffer.compressor is NoCompressor)
//
//            val queue = FieldValueQueue(app) {
//                // Ничего не вынимаем из очереди, только пихаем
//                return@FieldValueQueue Received.RETRY
//            }
//            // Ставим раздачу значений на паузу, чтобы очередь не пыталась отправить полученные данные.
//            // Нам здесь нужно, чтобы очередь только наполнялась.
//            queue.getIsPausedOnlyForTests().set(true)
//
//            // Пихаем в очередь 100 значений. Они все не влезут.
//            repeat(100) {
//                // Пихаем 99 байт данных + 1 байт на длину. В буфер 1000 байт должны влезть только 10 значений.
//                queue.add(FieldValue.fromString(String.format("%04d", it) + "~".repeat(95)))
//            }
//
//            // Пихали 100 сообщений, по 5 на чанк, получаем 20 чанков.
//            assertEquals(20, queue.getChunksOnlyForTests().size)
//
//            // Уничтожаем лишние byte arrays в буфере, некоторые weak ref остаются пустыми.
//            Runtime.getRuntime().gc()
//
//            // Теперь всё ещё все чанки на месте, потому что после GC не было нового чанка.
//            assertEquals(20, queue.getChunksOnlyForTests().size)
//
//            // Пихаем ещё одно значение. Должен создаться новый чанк, что приведёт к очистке мёртвых чанков.
//            queue.add(FieldValue.fromString("last added value"))
//
//            // queue.getChunksOnlyForTests().forEachIndexed { k, v ->
//            //     println("$k ${v.isReadable()}")
//            // }
//
//            // Вот теперь количество чанков уменьшилось.
//            // 1 чанк первый, он не буферизуется
//            // 2 чанка в буфере
//            // 1 чанк был последний на момент GC
//            // 1 чанк вот только что добавили
//            // Всего в очереди 5 чанков.
//            assertEquals(5, queue.getChunksOnlyForTests().size)
//        }
//    }

    @Test
    @Ignore
    fun testSendAndOverflow() {
        val config = "buffer { memory_compression off; memory_bytes 500; } queue_chunk_messages 5; from tcp 1211; to tcp 1212"

        val numberOfMessagesSent = 150

        makeApp(config).use { app ->
            val root = app.config.root

            // Запускаем систему, ждём инициализации
            root.start()
            Thread.sleep(100)

            val byteArraysAsStringsBeforeTest = getBufferContentDump(app.defaultBuffer)
            assertEquals(0, byteArraysAsStringsBeforeTest.size, "Before any testing our buffer should be empty")

            // Отправляем сообщения, пока что TCP не сможет никуда их отправить.
            // Поскольку все сообщения в буфер не влезут, должны остаться только последние 100 сообщений: 051..150
            val payloadsToSend = mutableListOf<String>()
            repeat(numberOfMessagesSent) {
                payloadsToSend.add(String.format("%03d", it) + "\n")
            }
            sendPayloadsByTcpClient(1211, payloadsToSend)


            // Чтобы буфер реально смог напороться на переполнение, должны протухнуть наши byte array в WeakReference.
            // Для этого надо дёрнуть особый рубильник.
            Runtime.getRuntime().gc()


            // Убедимся, что у нас в буфере лежат нужные нам сообщения.
            val bufferDumpList = getBufferContentDump(app.defaultBuffer)
            val bufferDump = bufferDumpList.joinToString("\n")

            assertEquals(20, bufferDumpList.size, "Expected number of buffer byte arrays does not match")
            assertEquals("<04>045\\n<04>046\\n<04>047\\n<04>048\\n<04>049\\n", bufferDumpList.firstOrNull(), "First buffer byte array does not match")

            // Каждый кусок содержит 5 сообщений, сообщение = 5 байт, это всего 25 байт.
            // Максимальный размер буфера 500 байт, то есть туда влезет 20 кусков, то есть 100 сообщений.
            val sb = StringBuilder()
            var i = 45 // 150 отправили, последние 5 штук не в буфере, получаем 150 - 5 - 100 = 45
            repeat(100 / 5) {
                repeat(5) {
                    sb.append("<04>").append(String.format("%03d", i)).append("\\n")
                    i++
                }
                sb.append("\n")
            }
            // Убираем последний \n, потому что его не будет в настоящем дампе.
            val expectedBufferDump = sb.toString().dropLast(1)

            assertEquals(expectedBufferDump, bufferDump)

            assertEquals(500, app.defaultBuffer.currentSizeInMemory.get(), "Expected buffer size does not match")


            // Окей, буфер в правильном состоянии, теперь разберёмся с очередями.

            // Очередь на приём должна быть пуста: всё уже принято и обработано и проехало по конвейеру.
            assertEquals(1, app.tcpListeners.listeners.size)
            val tcpListener = app.tcpListeners.listeners.values.first()
            assertNotNull(tcpListener)

            // Когда очередь пуста, в ней нет чанков. И наоборот.
//            val fromQueue = tcpListener.getQueueOnlyForTests()
//            assertEquals(0, fromQueue.getChunksOnlyForTests().size)


            // Очередь на отправку содержит всякое разное.
            assertEquals(1, app.tcpSenders.senders.size)
            val tcpSender = app.tcpSenders.senders.values.first()
            assertNotNull(tcpSender)

            // Тут в очереди должно быть много чанков, и все кроме двух должны быть буферизованы.
//            val toQueue = tcpSender.getQueueOnlyForTests()
//            val toChunks = toQueue.getChunksOnlyForTests()
//            assertEquals(30, toChunks.size)
//            for (chunkNumber in toChunks.indices) {
//                val chunk = toChunks[chunkNumber]
//                when (chunkNumber) {
//                    0, 29 -> {
//                        // Первый и последний чанк просто не буферизованы.
//                        assertEquals("not buffered", chunk.getBufferedStateOnlyForTests(), "Invalid buffered state in chunk $chunkNumber")
//                        assertEquals(5, chunk.getListOnlyForTests().size, "Non-buffered chunk $chunkNumber has an empty list")
//                    }
//                    else -> {
//                        // Все кроме первого и последнего чанка должны быть буферизованы.
//                        // Это значит, что сам чанк уже не имеет никаких данных, а только weak ref на место в буфере.
//                        assertEquals("buffered", chunk.getBufferedStateOnlyForTests(), "Invalid buffered state in chunk $chunkNumber")
//                        assertEquals(0, chunk.getListOnlyForTests().size, "Buffered chunk $chunkNumber has non-empty list")
//                        val weakRef = chunk.getByteArrayReferenceOnlyForTests()
//                        assertNotNull(weakRef)
//
//                        // У нас всего 30 чанков, минус 2 не буферизованы, и 20 массивов в буфере. Получаем 8 тухлых чанков: у них уже нет массива.
//                        if (chunkNumber in 1..8) {
//                            assertNull(weakRef.get())
//                        } else {
//                            assertNotNull(weakRef.get())
//                        }
//                    }
//                }
//            }


            // Теперь поднимаем TCP-сервер и принимаем накопленные в очереди сообщения.
            val payloads = receivePayloadsByTcpServer(1212)
            // println("Received some messages at TCP 1212:")
            // println(payloads)

            // Если мы всё правильно понимаем в жизни, приехать должны следующие сообщения:
            // 0..4 первый чанк
            // 45..144 буфер (500 байт хватило на 100 сообщений)
            // 145..149 последний чанк
            assertEquals(110, payloads.size, "Expected number of received messages does not match")
        }
    }

    private fun getBufferContentDump(dataBuffer: DataBuffer): List<String> {
        return dataBuffer.getByteArraysOnlyForTests()
            .toList()
            .map {
                it.joinToString("") { byte ->
                    when (byte) {
                        'c'.toByte() -> "c"
                        'a'.toByte() -> "a"
                        't'.toByte() -> "t"

                        '0'.toByte() -> "0"
                        '1'.toByte() -> "1"
                        '2'.toByte() -> "2"
                        '3'.toByte() -> "3"
                        '4'.toByte() -> "4"
                        '5'.toByte() -> "5"
                        '6'.toByte() -> "6"
                        '7'.toByte() -> "7"
                        '8'.toByte() -> "8"
                        '9'.toByte() -> "9"

                        '\n'.toByte() -> "\\n"
                        else -> "<" + String.format("%02X", byte) + ">"
                    }
                }
            }
    }

    private fun sendPayloadsByTcpClient(port: Int, payloads: List<String>) {
        Socket().use {
            // println("test socket client on port $port: created")
            it.connect(InetSocketAddress(InetAddress.getLocalHost(), port))
            // println("test socket client on port $port: connected")
            for (payload in payloads) {
                it.getOutputStream().write(payload.toByteArray())
                // Если тут не будет задержки, то сервер не принимает все отправленные сообщения.
                // Это хорошо бы отдельно разобрать, но пока что хочется хотя бы буфер протестировать.
                Thread.sleep(0, 1)
            }
            Thread.sleep(100)
            // println("test socket client on port $port: closing")
        }
        // println("test socket client on port $port: closed")
    }

    private fun receivePayloadsByTcpServer(port: Int): List<String> {
        val list = mutableListOf<String>()
        ServerSocket(port).use { server ->
            // println("test socket server on port $port: started")
            try {
                server.accept().use { connection ->
                    // println("test socket server on port $port: connected")
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
