package beholder.backend.http

import io.netty.channel.nio.NioEventLoopGroup
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.socket.nio.NioServerSocketChannel

class Server(val port: Int, val packageName: String) {
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
                    BasicAuthHandler(),
                    StaticContentHandler(packageName + ".web"),
                    WebSocketHttpHandler(),
                    webSocketRouter,
                    ErrorHandler()
                )))

            val ch = b.bind(port)?.sync()?.channel()

            println("Started HTTP server at 127.0.0.1:$port");

            ch?.closeFuture()?.sync()
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }

    }
}
