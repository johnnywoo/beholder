package beholder.backend

import beholder.backend.http.startServer
import java.util.logging.Logger
import java.util.logging.Level

fun main(args: Array<String>) {
    if (args.size == 0) {
        System.err.println("Usage: beholder.jar <port>")
        System.exit(1)
    }

    val websocketRouter = WebSocketRouter()
    websocketRouter.onAction("echo", javaClass<String>(), {
        ctx, action, data ->
            data as String
    })

    startServer(args[0].toInt(), "beholder.backend", websocketRouter)
}


fun Any.logInfo(message: String)
    = Logger.getLogger(this.javaClass.getName()).log(Level.INFO, message)

fun Any.logWarning(message: String)
    = Logger.getLogger(this.javaClass.getName()).log(Level.WARNING, message)

fun Any.logWarning(message: String, cause: Throwable?)
    = Logger.getLogger(this.javaClass.getName()).log(Level.WARNING, message, cause)
