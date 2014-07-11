package beholder.backend.http

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import com.google.gson.Gson
import com.google.gson.JsonParser
import beholder.backend.api.Message
import beholder.backend.fromJsonOrNull
import beholder.backend.config.UserConfiguration
import beholder.backend.gson
import io.netty.util.AttributeKey
import io.netty.channel.group.DefaultChannelGroup
import io.netty.util.concurrent.GlobalEventExecutor

Sharable class WebSocketRouter : SimpleChannelInboundHandler<TextWebSocketFrame>() {
    class object {
        val JSON_PARSER = JsonParser()
    }

    private class ActionListener(val parser: (String) -> Any?, val callback: (Connection, Any) -> Unit)

    private val actionListeners: MutableMap<String, ActionListener> = hashMapOf()

    fun onAction<T>(action: String, clazz: Class<T>, callback: (Connection, Any) -> Unit) {
        actionListeners.put(action, ActionListener({ gson.fromJsonOrNull(it, clazz) }, callback))
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: TextWebSocketFrame?) {
        val handshaker = ctx?.attr(WebSocketHttpHandler.CHANNEL_ATTR_HANDSHAKER)?.get()
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

        val message = gson.fromJsonOrNull(text, javaClass<Message>())
        if (message == null) {
            return ctx.tryNextHandler(msg)
        }

        val actionListener = actionListeners[message.action]
        if (actionListener == null) {
            return ctx.tryNextHandler(msg)
        }

        val data = actionListener.parser(text)
        if (data == null) {
            return ctx.tryNextHandler(msg)
        }

        actionListener.callback(Connection(ctx), data)
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext?) {
        ctx?.flush()
    }
}
