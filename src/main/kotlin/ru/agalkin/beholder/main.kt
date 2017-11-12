package ru.agalkin.beholder

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.time.LocalDateTime
import kotlin.concurrent.thread

fun main(args: Array<String>) {
    startUdpListener(3820) {
        println(LocalDateTime.now().toString() + " 3820: " + it)
    }
    startUdpListener(3821) {
        println(LocalDateTime.now().toString() + " 3821: " + it)
    }
}

fun startUdpListener(port: Int, threadName: String? = null, onReceive: (String) -> Unit) {
    println("starting UDP listener on port $port")

    val datagramSocket = DatagramSocket(port)

    // 10 мегабайт должно быть достаточно каждому
    val buffer = ByteArray(10 * 1024 * 1024)

    thread(name = threadName ?: "udp-listener-$port") {
        while (true) {
            val packet = DatagramPacket(buffer, buffer.size)
            datagramSocket.receive(packet)
            val text = String(packet.data, 0, packet.length)
            onReceive(text)
        }
    }
}