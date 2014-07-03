package beholder.http

import io.netty.channel.nio.NioEventLoopGroup
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.socket.nio.NioServerSocketChannel
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
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.channel.ChannelHandler.Sharable

fun startServer(port: Int) {
    val bossGroup   = NioEventLoopGroup(1)
    val workerGroup = NioEventLoopGroup()
    try {
        val b = ServerBootstrap()
        b.group(bossGroup, workerGroup)
            ?.channel(javaClass<NioServerSocketChannel>())
            //?.handler(LoggingHandler(LogLevel.INFO))
            ?.childHandler(ServerInitializer())

        val ch = b.bind(port)?.sync()?.channel()

        println("Started HTTP server at 127.0.0.1:$port");

        ch?.closeFuture()?.sync()
    } finally {
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }
}

class ServerInitializer : ChannelInitializer<SocketChannel>() {
    class object {
        val SERVER_HANDLER = ServerHandler()
    }

    override fun initChannel(ch: SocketChannel?): Unit {
        val p = ch?.pipeline()
        p?.addLast(HttpServerCodec())
        p?.addLast(HttpObjectAggregator(1048576)) // aggregate HttpContents into a single FullHttpRequest, maxContentLength = 1mb
        p?.addLast(SERVER_HANDLER)
    }
}

Sharable class ServerHandler : ChannelInboundHandlerAdapter() {
    val CONTENT = "Hello World".getBytes()

    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?): Unit {
        if (msg !is HttpRequest)
            return

        if (HttpHeaders.is100ContinueExpected(msg)) {
            ctx?.write(DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE))
        }

        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(CONTENT))
        response.headers()?.set(Names.CONTENT_TYPE, "text/plain")
        response.headers()?.set(Names.CONTENT_LENGTH, response.content()?.readableBytes())

        if (!HttpHeaders.isKeepAlive(msg)) {
            ctx?.write(response)?.addListener(ChannelFutureListener.CLOSE)
        } else {
            response.headers()?.set(Names.CONNECTION, Values.KEEP_ALIVE)
            ctx?.write(response)
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
