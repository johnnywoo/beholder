package beholder.backend.http

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.SimpleChannelInboundHandler
import java.util.logging.Logger
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame

Sharable class WebSocketHandler : SimpleChannelInboundHandler<WebSocketFrame>() {
    override fun channelRead0(ctx: ChannelHandlerContext?, msg: WebSocketFrame?) {
        val handshaker = ctx?.attr(WebSocketHttpHandler.CHANNEL_ATTR_HANDSHAKER)?.get()
        if (ctx == null || handshaker == null) {
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

