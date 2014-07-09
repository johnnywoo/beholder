package beholder.backend

import beholder.backend.http.startServer
import java.util.logging.Logger
import java.util.logging.Level
import beholder.backend.http.WebSocketRouter
import beholder.backend.api.EchoMessage
import beholder.backend.api.LoginMessage
import io.netty.channel.group.DefaultChannelGroup
import io.netty.util.concurrent.GlobalEventExecutor
import io.netty.util.AttributeKey
import io.netty.channel.ChannelHandlerContext

val CHANNEL_ATTR_API_KEY: AttributeKey<String>? = AttributeKey.valueOf("apiKey")
val clientChannelGroup = DefaultChannelGroup(GlobalEventExecutor.INSTANCE)

fun main(args: Array<String>) {
    if (args.size == 0) {
        System.err.println("Usage: beholder.jar <port>")
        System.exit(1)
    }

    val websocketRouter = WebSocketRouter()
    websocketRouter.onAction("echo", javaClass<EchoMessage>(), {
        ctx, data ->
            val channel = ctx.channel()
            val apiKey = channel?.attr(CHANNEL_ATTR_API_KEY)?.get()
            val text = if (apiKey == null) "not authorized" else (data as EchoMessage).data
            EchoMessage(text)
    })

    websocketRouter.onAction("login", javaClass<LoginMessage>(), {
        ctx, data ->
            loginAction(ctx, data as LoginMessage)
            null
    })

    startServer(args[0].toInt(), "beholder.backend", websocketRouter)
}

fun loginAction(ctx: ChannelHandlerContext, data: LoginMessage) {
    val apiKey = data.apiKey
    val channel = ctx.channel()
    if (channel == null) {
        return
    }
    channel.attr(CHANNEL_ATTR_API_KEY)?.set(apiKey)
    clientChannelGroup.add(channel)
}


fun Any.log(message: String)
    = Logger.getLogger(this.javaClass.getName()).log(Level.INFO, message)

fun Any.logWarning(message: String, cause: Throwable?)
    = Logger.getLogger(this.javaClass.getName()).log(Level.WARNING, message, cause)
