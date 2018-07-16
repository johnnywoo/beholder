package ru.agalkin.beholder

import java.io.InputStream
import java.io.InputStreamReader
import java.net.SocketException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun d(message: String) {
    println("[" + Thread.currentThread().name + "] " + message)
}

fun readTextFromResource(name: String): String {
    val inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(name)
    return InputStreamReader(inputStream).readText()
}

fun charListToString(list: List<Char>): String {
    val sb = StringBuilder(list.size)
    for (char in list) {
        sb.append(char)
    }
    return sb.toString()
}

fun <T> listToString(list: List<T>, convert: (T) -> String): String {
    val sb = StringBuilder()
    for (element in list) {
        sb.append(convert(element))
    }
    return sb.toString()
}

fun addNewlineIfNeeded(text: String)
    = when (text.isEmpty() || text.last() != '\n') {
        true  -> text + "\n"
        false -> text
    }

fun substringUpTo(string: String, maxLength: Int): String {
    if (string.length > maxLength) {
        return string.substring(0, maxLength)
    }
    return string
}

fun readInputStreamAndDiscard(inputStream: InputStream, threadName: String) {
    // ignore any input from the process
    thread(isDaemon = true, name = threadName) {
        val devNull = ByteArray(1024)
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
                break
            }
        }
    }
}

fun defaultString(string: String?, default: String)
    = if (string != null && !string.isEmpty()) string else default

fun getSystemHostname(): String? {
    try {
        val builder = ProcessBuilder("hostname")
        builder.redirectOutput(ProcessBuilder.Redirect.PIPE)
        builder.redirectError(ProcessBuilder.Redirect.INHERIT)

        val process = builder.start()
        process.waitFor(10, TimeUnit.SECONDS)
        val hostname = process.inputStream.bufferedReader().readText().trim()
        if (hostname.isEmpty()) {
            return null
        }
        return hostname
    } catch (e: Throwable) {
        InternalLog.exception(e)
        return null
    }
}

fun <T> threadLocal(initializer: () -> T): ThreadLocalDelegate<T> = ThreadLocalDelegate(initializer)

class ThreadLocalDelegate<T>(private val initializer: () -> T) : ReadOnlyProperty<Any, T> {
    private val holder = object : ThreadLocal<T>() {
        override fun initialValue()
            = initializer()
    }
    override fun getValue(thisRef: Any, property: KProperty<*>): T
        = holder.get()
}
