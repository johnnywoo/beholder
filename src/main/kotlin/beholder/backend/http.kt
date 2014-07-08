package beholder.backend.http

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
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame
import io.netty.channel.ChannelHandler
import java.util.HashMap
import beholder.backend.WebsocketRouter
import io.netty.util.CharsetUtil
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame

fun startServer(port: Int, packageName: String) {
    val bossGroup   = NioEventLoopGroup(1)
    val workerGroup = NioEventLoopGroup()
    try {
        val b = ServerBootstrap()
        val websocketRouter = WebsocketRouter()
        b.group(bossGroup, workerGroup)
            ?.channel(javaClass<NioServerSocketChannel>())
            //?.handler(LoggingHandler(LogLevel.INFO))
            ?.childHandler(ServerInitializer(arrayListOf(
                WebsocketHttpHandler(),
                StaticContentHandler(packageName + ".web"),
                WebsocketHandler(),
                websocketRouter,
                ErrorHandler()
            )))

        websocketRouter.onAction("echo", javaClass<String>(), {
            ctx, action, data ->
                val text = data as String
                ctx.channel()?.writeAndFlush(TextWebSocketFrame(text))
        })

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

class StaticResource(val path: String) {
    class object {
        fun exists(path: String): Boolean {
            // TODO we need a better way to distinguish directories from files
            if (!path.contains(".")) {
                return false
            }
            return this.javaClass.getResource(path) != null
        }
    }
    fun getContentType()
        = when (path.substringAfterLast(".")) {
        "html" -> "text/html"
        "js"   -> "text/javascript"
        "map"  -> "application/json"
        else   -> "text/plain"
    }
    fun getContent(): ByteArray {
        val inputStream = this.javaClass.getResourceAsStream(path)
        try {
            return inputStream?.readBytes() ?: ByteArray(0)
        } finally {
            inputStream?.close()
        }
    }
}

class StaticResourceProvider(val resourcesPackageName: String, val storage: HashMap<String, StaticResource> = hashMapOf()) {
    fun get(uri: String): StaticResource? {
        if (!storage.containsKey(uri)) {
            val resourcePath = "/" + resourcesPackageName.replace(".", "/") + uri // /blah/blah.html -> /beholder/web/blah/blah.html
            if (StaticResource.exists(resourcePath)) {
                storage[uri] = StaticResource(resourcePath)
            }
        }
        return storage[uri]
    }
}

Sharable class StaticContentHandler(val resourcesPackageName: String) : SimpleChannelInboundHandler<FullHttpRequest>() {
    val staticContent = StaticResourceProvider(resourcesPackageName)

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: FullHttpRequest?) {
        if (msg != null && msg.isSuccess && msg.isMethodGet) {
            val uri = msg.getUri()?.substringBefore("?") ?: ""
            val resource = staticContent[uri]
            if (resource != null) {
                ctx?.sendHttpResponse(msg, resource.getContent(), HttpResponseStatus.OK, resource.getContentType())
                return
            }
            val resourceIndex = staticContent[uri.addUriPathComponent("index.html")]
            if (resourceIndex != null) {
                ctx?.sendHttpResponse(msg, resourceIndex.getContent(), HttpResponseStatus.OK, resourceIndex.getContentType())
                return
            }
        }

        ctx?.fireChannelRead(msg?.retain())
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

        fun getWebsocketLocation(request: FullHttpRequest)
            = "ws://" + request.headers()?.get(HttpHeaders.Names.HOST) + WEBSOCKET_PATH
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: FullHttpRequest?) {
        if (msg == null || !msg.isSuccess) {
            ctx?.fireChannelRead(msg?.retain())
            return
        }

        if (!msg.isMethodGet) {
            ctx?.fireChannelRead(msg.retain())
            return
        }

        if (!WEBSOCKET_PATH.equals(msg.getUri())) {
            ctx?.fireChannelRead(msg.retain())
            return
        }

        val webSocketFactory = WebSocketServerHandshakerFactory(getWebsocketLocation(msg), null, false)
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

        ctx.fireChannelRead(msg?.retain())
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext?) {
        ctx?.flush()
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

        if (msg is FullHttpRequest && msg.isSuccess) {
            ctx?.sendHttpResponse(null, "Not found", HttpResponseStatus.NOT_FOUND)
            return
        }

        ctx?.sendHttpResponse(null, "", HttpResponseStatus.BAD_REQUEST)
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext?) {
        ctx?.flush()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        LOGGER.log(Level.WARNING, "Exception caught", cause)
        ctx?.close()
    }
}


//
// SUGAR
//

fun String.addUriPathComponent(component: String)
    = this + (if (this.endsWith("/")) "" else "/") + component

val FullHttpRequest.isSuccess: Boolean
    get() = this.getDecoderResult()?.isSuccess() ?: false

val FullHttpRequest.isMethodGet: Boolean
    get() = this.getMethod()?.equals(HttpMethod.GET) ?: false


val FullHttpResponse.contentLength: Long
    get() = this.content()?.readableBytes()?.toLong() ?: 0


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
