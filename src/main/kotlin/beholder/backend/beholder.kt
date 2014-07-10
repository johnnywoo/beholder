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
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import beholder.backend.config.Configuration
import beholder.backend.config.UserConfiguration

val clientChannelGroup = DefaultChannelGroup(GlobalEventExecutor.INSTANCE)
val configuration      = Configuration("beholder")

fun main(args: Array<String>) {
    if (args.size == 0) {
        System.err.println("Usage: beholder.jar <port>")
        System.exit(1)
    }

    val websocketRouter = WebSocketRouter()
    websocketRouter.onAction("echo", javaClass<EchoMessage>(), {
        ctx, data ->
            val text = if (!ctx.isRegistered) "not authorized" else (data as EchoMessage).data
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
    if (ctx.isRegistered) {
        return
    }

    val userConfiguration = configuration.getUserConfigurationByApiKey(data.apiKey)
    if (userConfiguration == null) {
        return
    }

    ctx.userConfiguration = userConfiguration

    val channel = ctx.channel()
    if (channel == null) {
        return
    }
    clientChannelGroup.add(channel)
}


fun Any.log(message: String)
    = Logger.getLogger(this.javaClass.getName()).log(Level.INFO, message)
fun Any.logWarning(message: String, cause: Throwable?)
    = Logger.getLogger(this.javaClass.getName()).log(Level.WARNING, message, cause)

[suppress("BASE_WITH_NULLABLE_UPPER_BOUND")] // TODO wtf?!
fun Gson.fromJsonOrNull<T>(json: String?, classOfT: Class<T>): T?
    = try { this.fromJson(json, classOfT) } catch (e: JsonSyntaxException) { null }

val CHANNEL_ATTR_USER_CONFIGURATION: AttributeKey<UserConfiguration>? = AttributeKey.valueOf("userConfiguration")
val ChannelHandlerContext.isRegistered: Boolean
    get() = this.hasAttr(CHANNEL_ATTR_USER_CONFIGURATION)
var ChannelHandlerContext.userConfiguration: UserConfiguration?
    get() = this.attr(CHANNEL_ATTR_USER_CONFIGURATION)?.get()
    set(userConfiguration: UserConfiguration?) {
        if (userConfiguration == null) {
            this.attr(CHANNEL_ATTR_USER_CONFIGURATION)?.remove()
        } else {
            this.attr(CHANNEL_ATTR_USER_CONFIGURATION)?.set(userConfiguration)
        }
    }
