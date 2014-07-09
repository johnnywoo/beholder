package beholder.backend.http

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import com.google.gson.Gson
import com.google.gson.JsonParser
import beholder.backend.api.Message

Sharable class WebSocketRouter : SimpleChannelInboundHandler<TextWebSocketFrame>() {
    class object {
        val GSON        = Gson()
        val JSON_PARSER = JsonParser()
    }

    private class ActionListener(val parser: (String) -> Any, val callback: (ChannelHandlerContext, Any) -> Message?)

    private val actionListeners: MutableMap<String, ActionListener> = hashMapOf()

    fun onAction<T>(action: String, clazz: Class<T>, callback: (ChannelHandlerContext, Any) -> Message?) {
        actionListeners.put(action, ActionListener({ GSON.fromJson(it, clazz) as Any }, callback))
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: TextWebSocketFrame?) {
        val handshaker = ctx?.channel()?.attr(WebSocketHttpHandler.CHANNEL_ATTR_HANDSHAKER)?.get()
        if (ctx == null || handshaker == null) {
            throw RuntimeException("No handshaker for incoming websocket frame")
        }

        if (msg == null) {
            return ctx.tryNextHandler(msg)
        }

        val text = msg.text()
        if (text == null) {
            return ctx.tryNextHandler(msg)
        }

        val message = GSON.fromJson(text, javaClass<Message>())
        if (message == null) {
            return ctx.tryNextHandler(msg)
        }

        val actionListener = actionListeners[message.action]
        if (actionListener == null) {
            return ctx.tryNextHandler(msg)
        }

        val response = actionListener.callback(ctx, actionListener.parser(text))
        if (response != null) {
            ctx.channel()?.writeAndFlush(TextWebSocketFrame(GSON.toJson(response)))
        }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext?) {
        ctx?.flush()
    }
}
