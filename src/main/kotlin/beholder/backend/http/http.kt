package beholder.backend.http

import io.netty.channel.nio.NioEventLoopGroup
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpMethod
import io.netty.buffer.ByteBufHolder
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpVersion
import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpHeaders.Names
import io.netty.channel.ChannelFutureListener

fun startServer(port: Int, packageName: String, webSocketRouter: WebSocketRouter) {
    val bossGroup   = NioEventLoopGroup(1)
    val workerGroup = NioEventLoopGroup()
    try {
        val b = ServerBootstrap()
        b.group(bossGroup, workerGroup)
            ?.channel(javaClass<NioServerSocketChannel>())
            //?.handler(LoggingHandler(LogLevel.INFO))
            ?.childHandler(ServerInitializer(arrayListOf(
                WebSocketHttpHandler(),
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

class ServerInitializer(val handlers: List<ChannelHandler>) : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel?): Unit {
        val p = ch?.pipeline()

        p?.addLast(HttpServerCodec())
        p?.addLast(HttpObjectAggregator(1024 * 1024)) // aggregate HttpContents into a single FullHttpRequest, maxContentLength = 1mb

        for (handler in handlers) {
            p?.addLast(handler)
        }
    }
}


//
// SUGAR
//

val FullHttpRequest.isSuccess: Boolean
    get() = this.decoderResult()?.isSuccess() ?: false

val FullHttpRequest.isMethodGet: Boolean
    get() = this.method()?.equals(HttpMethod.GET) ?: false


val FullHttpResponse.contentLength: Long
    get() = this.content()?.readableBytes()?.toLong() ?: 0


fun ChannelHandlerContext.tryNextHandler(message: ByteBufHolder?) {
    this.fireChannelRead(message?.retain())
}

fun ChannelHandlerContext.sendHttpResponse(request: FullHttpRequest?, content: String, status: HttpResponseStatus = HttpResponseStatus.OK, contentType: String = "text/plain")
    = this.sendHttpResponse(request, content.getBytes(), status, contentType)

fun ChannelHandlerContext.sendHttpResponse(request: FullHttpRequest?, content: ByteArray, status: HttpResponseStatus = HttpResponseStatus.OK, contentType: String = "text/plain") {
    var response: FullHttpResponse

    // generate an error page if response.getStatus() is not OK (200) and content is empty
    val isBadStatus = !status.equals(HttpResponseStatus.OK)
    if (isBadStatus && content.isEmpty()) {
        response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(status.toString(), CharsetUtil.UTF_8))
    } else {
        response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.wrappedBuffer(content))
    }

    // send the response and close the connection if necessary
    HttpHeaders.setContentLength(response, response.contentLength)
    response.headers()?.set(Names.CONTENT_TYPE, contentType)
    val writeFuture = this.channel()?.writeAndFlush(response)
    if (isBadStatus || !HttpHeaders.isKeepAlive(request)) {
        writeFuture?.addListener(ChannelFutureListener.CLOSE)
    }
}
