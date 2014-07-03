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
import io.netty.handler.codec.http.DefaultFullHttpResponse

import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpHeaders.Names
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpRequest
import java.util.logging.Logger
import java.util.logging.Level
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.util.CharsetUtil

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

Sharable class ServerHandler : SimpleChannelInboundHandler<FullHttpRequest>() {
    class object {
        val LOGGER = Logger.getLogger(ServerHandler.javaClass.getName())

        val CONTENT = "Hello World".getBytes()

        fun sendHttpResponse(ctx: ChannelHandlerContext?, response: FullHttpResponse, isKeepAlive: Boolean) {
            sendHttpResponse(ctx, response, isKeepAlive, "text/plain")
        }

        fun sendHttpResponse(ctx: ChannelHandlerContext?, response: FullHttpResponse, isKeepAlive: Boolean, contentType: String) {
            // generate an error page if response.getStatus() is not OK (200) and content is empty
            if (response.getStatus()?.code() != 200) {
                if (response.content()?.readableBytes() == 0) {
                    val buf = Unpooled.copiedBuffer(response.getStatus().toString(), CharsetUtil.UTF_8)
                    response.content()?.writeBytes(buf)
                    buf?.release()
                }
            }

            // send the response and close the connection if necessary
            HttpHeaders.setContentLength(response, response.content()?.readableBytes()?.toLong()!!)
            response.headers()?.set(Names.CONTENT_TYPE, contentType)
            val writeFuture = ctx?.channel()?.writeAndFlush(response)
            if (!isKeepAlive || !response.getStatus().equals(HttpResponseStatus.OK)) {
                writeFuture?.addListener(ChannelFutureListener.CLOSE)
            }
        }
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: FullHttpRequest?) {
        sendHttpResponse(ctx, DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(CONTENT)), HttpHeaders.isKeepAlive(msg))
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext?) {
        ctx?.flush()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        LOGGER.log(Level.WARNING, "Exception caught", cause)
        ctx?.close()
    }
}
