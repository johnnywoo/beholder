package beholder.backend.http

import io.netty.channel.nio.NioEventLoopGroup
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.socket.nio.NioServerSocketChannel
import beholder.backend.config.Configuration
import beholder.backend.having

class Server(val configuration: Configuration, val packageName: String) {
    val webSocketRouter = WebSocketRouter()

    fun onAction<T>(action: String, clazz: Class<T>, callback: (Connection, Any) -> Unit) {
        webSocketRouter.onAction(action, clazz, callback)
    }

    fun start() {
        val bossGroup   = NioEventLoopGroup(1)
        val workerGroup = NioEventLoopGroup()
        try {
            val b = ServerBootstrap()
            b.group(bossGroup, workerGroup)
                ?.channel(javaClass<NioServerSocketChannel>())
                //?.handler(LoggingHandler(LogLevel.INFO))
                ?.childHandler(ServerInitializer(arrayListOf(
                    WebSocketHttpHandler(),
                    BasicAuthHandler({
                        login, password ->
                            configuration.getUserConfiguration(login)?.having({ it.password == password })?.apiKey
                    }),
                    StaticContentHandler(packageName + ".web"),
                    WebSocketHttpHandler(),
                    webSocketRouter,
                    ErrorHandler()
                )))

            val ch = b.bind(configuration.port)?.sync()?.channel()

            println("Started HTTP server at 127.0.0.1:" + configuration.port);

            ch?.closeFuture()?.sync()
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }

    }
}
