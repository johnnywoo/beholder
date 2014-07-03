package beholder

import beholder.http.startServer

fun main(args: Array<String>) {
    if (args.size == 0) {
        println("Usage: beholder.jar <port>")
        return
    }
    startServer(args[0].toInt())
}
