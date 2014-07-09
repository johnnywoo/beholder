package beholder.backend.http

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.SimpleChannelInboundHandler
import java.util.logging.Logger
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.util.AttributeKey
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory

import beholder.backend.http.isSuccess
import beholder.backend.http.isMethodGet
import beholder.backend.log

Sharable class WebSocketHttpHandler : SimpleChannelInboundHandler<FullHttpRequest>() {
    class object {
        val CHANNEL_ATTR_HANDSHAKER: AttributeKey<WebSocketServerHandshaker>? = AttributeKey.valueOf("handshaker")
        val WEBSOCKET_PATH = "/ws"

        fun getWebSocketLocation(request: FullHttpRequest)
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

        val webSocketFactory = WebSocketServerHandshakerFactory(getWebSocketLocation(msg), null, false)
        val handshaker       = webSocketFactory.newHandshaker(msg)

        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx?.channel())
            return
        }

        handshaker.handshake(ctx?.channel(), msg)
        log("WebSocket handshaked, channel " + ctx?.channel().toString())
        ctx?.channel()?.attr(CHANNEL_ATTR_HANDSHAKER)?.set(handshaker)
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext?) {
        ctx?.flush()
    }
}
