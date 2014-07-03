package beholder

import beholder.http.startServer

fun main(args: Array<String>) {
    if (args.size == 0) {
        System.err.println("Usage: beholder.jar <port>")
        System.exit(1)
    }

    startServer(args[0].toInt())
}
