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
import beholder.backend.user.UserConfiguration
import io.netty.channel.Channel

val clientChannelGroup = DefaultChannelGroup(GlobalEventExecutor.INSTANCE)

fun main(args: Array<String>) {
    if (args.size == 0) {
        System.err.println("Usage: beholder.jar <port>")
        System.exit(1)
    }

    val websocketRouter = WebSocketRouter()
    websocketRouter.onAction("echo", javaClass<EchoMessage>(), {
        ctx, data ->
            val isRegistered = ctx.channel()?.isRegistered ?: false
            val text = if (!isRegistered) "not authorized" else (data as EchoMessage).data
            EchoMessage(text)
    })

    websocketRouter.onAction("login", javaClass<LoginMessage>(), {
        ctx, data ->
            login(ctx, data as LoginMessage)
            null
    })

    startServer(args[0].toInt(), "beholder.backend", websocketRouter)
}

fun login(ctx: ChannelHandlerContext, data: LoginMessage) {
    val channel = ctx.channel()
    if (channel == null || channel.isRegistered) {
        return
    }

    val userConfiguration = findUserConfiguration(data.apiKey)
    if (userConfiguration == null) {
        return
    }

    channel.userConfiguration = userConfiguration
    clientChannelGroup.add(channel)
}

fun findUserConfiguration(apiKey: String): UserConfiguration? {
    val userConfiguration = UserConfiguration()
    userConfiguration.userName = apiKey
    userConfiguration.apiKey = apiKey
    return userConfiguration
}


fun Any.log(message: String)
    = Logger.getLogger(this.javaClass.getName()).log(Level.INFO, message)
fun Any.logWarning(message: String, cause: Throwable?)
    = Logger.getLogger(this.javaClass.getName()).log(Level.WARNING, message, cause)

val CHANNEL_ATTR_USER_CONFIGURATION: AttributeKey<UserConfiguration>? = AttributeKey.valueOf("userConfiguration")
val Channel.isRegistered: Boolean
    get() = this.attr(CHANNEL_ATTR_USER_CONFIGURATION)?.get() != null
var Channel.userConfiguration: UserConfiguration?
    get() = this.attr(CHANNEL_ATTR_USER_CONFIGURATION)?.get()
    set(userConfiguration: UserConfiguration?) {
        if (userConfiguration == null) {
            this.attr(CHANNEL_ATTR_USER_CONFIGURATION)?.remove()
        } else {
            this.attr(CHANNEL_ATTR_USER_CONFIGURATION)?.set(userConfiguration)
        }
    }
