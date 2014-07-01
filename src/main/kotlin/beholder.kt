package beholder

import io.netty.channel.nio.NioEventLoopGroup
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelOption
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.logging.LogLevel
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.channel.ChannelInitializer

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpRequest

import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpHeaders.Values
import io.netty.handler.codec.http.HttpHeaders.Names

fun main(args: Array<String>) {
    val PORT = 3822

    // Configure the server.
    val bossGroup   = NioEventLoopGroup(1)
    val workerGroup = NioEventLoopGroup()
    try {
        val b = ServerBootstrap()
        b.option(ChannelOption.SO_BACKLOG, 1024)
        b.group(bossGroup, workerGroup)
            ?.channel(javaClass<NioServerSocketChannel>())
            ?.handler(LoggingHandler(LogLevel.INFO))
            ?.childHandler(ServerInitializer())

        val ch = b.bind(PORT)?.sync()?.channel()

        System.err.println("Open your web browser and navigate to http://127.0.0.1:$PORT/");

        ch?.closeFuture()?.sync()
    } finally {
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }
}

class ServerInitializer : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel?): Unit {
        val p = ch?.pipeline();
        p?.addLast(HttpServerCodec());
        p?.addLast(ServerHandler());
    }
}

class ServerHandler : ChannelInboundHandlerAdapter() {
    val CONTENT = "Hello World".getBytes()

    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?): Unit {
        if (msg is HttpRequest) {
            if (HttpHeaders.is100ContinueExpected(msg)) {
                ctx?.write(DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE))
            }
            val keepAlive = HttpHeaders.isKeepAlive(msg)
            val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(CONTENT))
            response.headers()?.set(Names.CONTENT_TYPE, "text/plain")
            response.headers()?.set(Names.CONTENT_LENGTH, response.content()?.readableBytes())

            if (!keepAlive) {
                ctx?.write(response)?.addListener(ChannelFutureListener.CLOSE)
            } else {
                response.headers()?.set(Names.CONNECTION, Values.KEEP_ALIVE)
                ctx?.write(response)
            }
        }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext?) {
        ctx?.flush()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        cause?.printStackTrace()
        ctx?.close()
    }
}
