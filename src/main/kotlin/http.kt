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
import io.netty.util.AttributeKey
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpRequest
import java.util.logging.Logger
import java.util.logging.Level
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.util.CharsetUtil
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame

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
        val WEBSOCKET_HTTP_HANDLER = WebsocketHttpHandler()
        val STATIC_CONTENT_HANDLER = StaticContentHandler()
        val WEBSOCKET_HANDLER      = WebsocketHandler()
        val ERROR_HANDLER          = ErrorHandler()
    }

    override fun initChannel(ch: SocketChannel?): Unit {
        val p = ch?.pipeline()
        p?.addLast(HttpServerCodec())
        p?.addLast(HttpObjectAggregator(1024 * 1024)) // aggregate HttpContents into a single FullHttpRequest, maxContentLength = 1mb
        p?.addLast(WEBSOCKET_HTTP_HANDLER)
        p?.addLast(STATIC_CONTENT_HANDLER)
        p?.addLast(WEBSOCKET_HANDLER)
        p?.addLast(ERROR_HANDLER)
    }
}

fun sendHttpResponse(ctx: ChannelHandlerContext?, response: FullHttpResponse, isKeepAlive: Boolean, contentType: String = "text/plain") {
    // generate an error page if response.getStatus() is not OK (200) and content is empty
    if (!response.getStatus()?.equals(HttpResponseStatus.OK)!! && response.content()?.readableBytes() == 0) {
        val buf = Unpooled.copiedBuffer(response.getStatus().toString(), CharsetUtil.UTF_8)
        response.content()?.writeBytes(buf)
        buf?.release()
    }

    // send the response and close the connection if necessary
    HttpHeaders.setContentLength(response, response.getContentLength())
    response.headers()?.set(Names.CONTENT_TYPE, contentType)
    val writeFuture = ctx?.channel()?.writeAndFlush(response)
    if (!isKeepAlive || !response.getStatus().equals(HttpResponseStatus.OK)) {
        writeFuture?.addListener(ChannelFutureListener.CLOSE)
    }
}

Sharable class ErrorHandler : SimpleChannelInboundHandler<Any>() {
    class object {
        val LOGGER = Logger.getLogger(ErrorHandler.javaClass.getName())
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: Any?) {
        if (msg == null) {
            return
        }

        sendHttpResponse(ctx, DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST), false)
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext?) {
        ctx?.flush()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        LOGGER.log(Level.WARNING, "Exception caught", cause)
        ctx?.close()
    }
}

Sharable class StaticContentHandler : SimpleChannelInboundHandler<FullHttpRequest>() {
    override fun channelRead0(ctx: ChannelHandlerContext?, msg: FullHttpRequest?) {
        if (msg == null || !msg.isSuccess()) {
            ctx?.fireChannelRead(msg?.retain())
            return
        }

        if (!msg.isMethodGet()) {
            ctx?.fireChannelRead(msg.retain())
            return
        }

        val path = msg.getUri()
        // TODO static
        //sendHttpResponse(ctx, DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(CONTENT)), HttpHeaders.isKeepAlive(msg))

        ctx?.fireChannelRead(msg.retain())
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext?) {
        ctx?.flush()
    }
}

Sharable class WebsocketHttpHandler : SimpleChannelInboundHandler<FullHttpRequest>() {
    class object {
        val CHANNEL_ATTR_HANDSHAKER: AttributeKey<WebSocketServerHandshaker>? = AttributeKey.valueOf("handshaker")
        val WEBSOCKET_PATH = "/ws"
        val LOGGER = Logger.getLogger(WebsocketHttpHandler.javaClass.getName())

        fun getWebSocketLocation(request: FullHttpRequest): String {
            return "ws://" + request.headers()?.get(HttpHeaders.Names.HOST) + WEBSOCKET_PATH
        }
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: FullHttpRequest?) {
        if (msg == null || !msg.isSuccess()) {
            ctx?.fireChannelRead(msg?.retain())
            return
        }

        if (!msg.isMethodGet()) {
            ctx?.fireChannelRead(msg.retain())
            return
        }

        if (!WEBSOCKET_PATH.equals(msg.getUri())) {
            ctx?.fireChannelRead(msg.retain())
            return
        }

        val webSocketFactory = WebSocketServerHandshakerFactory(getWebSocketLocation(msg), null, false)
        val handshaker       = webSocketFactory.newHandshaker(msg)

        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx?.channel())
            return
        }

        handshaker.handshake(ctx?.channel(), msg)
        LOGGER.log(Level.INFO, "WebSocket handshaked, channel " + ctx?.channel().toString())
        ctx?.channel()?.attr(CHANNEL_ATTR_HANDSHAKER)?.set(handshaker)
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext?) {
        ctx?.flush()
    }
}

Sharable class WebsocketHandler : SimpleChannelInboundHandler<WebSocketFrame>() {
    class object {
        val LOGGER = Logger.getLogger(WebsocketHandler.javaClass.getName())
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: WebSocketFrame?) {
        val handshaker = ctx!!.channel()?.attr(WebsocketHttpHandler.CHANNEL_ATTR_HANDSHAKER)?.get()
        if (handshaker == null) {
            throw RuntimeException("No handshaker for incoming websocket frame")
        }

        if (msg is CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), msg.retain())
            return
        }

        if (msg is PingWebSocketFrame) {
            ctx.channel()?.write(PongWebSocketFrame(msg.content()?.retain()))
            return
        }

        if (msg !is TextWebSocketFrame) {
            ctx.fireChannelRead(msg?.retain())
            return
        }

        val text = msg.text()
        LOGGER.log(Level.INFO, "Incoming websocket frame: ${text}")
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext?) {
        ctx?.flush()
    }
}


//
// SUGAR
//

fun FullHttpRequest.isSuccess()   = this.getDecoderResult()?.isSuccess() ?: false
fun FullHttpRequest.isMethodGet() = this.getMethod()?.equals(HttpMethod.GET) ?: false

fun FullHttpResponse.getContentLength() = this.content()?.readableBytes()?.toLong() ?: 0
