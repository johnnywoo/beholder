package beholder.backend

import beholder.backend.http.startServer
import beholder.backend.http.WebSocketRouter
import beholder.backend.api.EchoMessage
import beholder.backend.api.LoginMessage
import io.netty.channel.group.DefaultChannelGroup
import io.netty.util.concurrent.GlobalEventExecutor
import io.netty.util.AttributeKey
import io.netty.channel.ChannelHandlerContext
import beholder.backend.config.Configuration
import beholder.backend.config.UserConfiguration

val clientChannelGroup = DefaultChannelGroup(GlobalEventExecutor.INSTANCE)
val configuration      = Configuration("beholder")

fun main(args: Array<String>) {
    if (args.size == 0) {
        System.err.println("Usage: beholder.sh (daemon|user)")
        System.exit(1)
    }

    val command = args[0]
    when (command) {
        "daemon" -> daemon()
        "user" -> createUser(getArg(args, 1), getArg(args, 2))
        else -> {
            System.err.println("Usage: beholder.sh (daemon|user)")
            System.exit(1)
        }
    }
}

fun getArg(args: Array<String>, index: Int): String? {
    if (args.size > index) {
        return args[index]
    }
    return null
}

fun daemon() {
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

    startServer(configuration.port, "beholder.backend", websocketRouter)
}

fun createUser(userName: String?, password: String?) {
    if (userName == null || password == null) {
        System.err.println("Usage: beholder.sh user <username> <password>")
        System.exit(1)
        return
    }
    val userConfiguration = UserConfiguration()
    userConfiguration.userName = userName
    userConfiguration.password = password
    configuration.saveUserConfiguration(userConfiguration)
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
