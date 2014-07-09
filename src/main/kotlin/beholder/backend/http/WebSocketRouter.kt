package beholder.backend.http

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonElement

Sharable class WebSocketRouter : SimpleChannelInboundHandler<TextWebSocketFrame>() {
    class object {
        val GSON        = Gson()
        val JSON_PARSER = JsonParser()
    }

    private class ActionListener(val parser: (JsonElement) -> Any, val callback: (ChannelHandlerContext, String, Any) -> String?)

    private val actionListeners: MutableMap<String, ActionListener> = hashMapOf()

    fun onAction<T>(action: String, clazz: Class<T>, callback: (ChannelHandlerContext, String, Any) -> String?) {
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

        val jsonElement = JSON_PARSER.parse(msg.text())
        if (jsonElement == null || !jsonElement.isJsonObject()) {
            return ctx.tryNextHandler(msg)
        }

        val rootJsonObject = jsonElement.getAsJsonObject()

        val action = rootJsonObject?.get("action")?.getAsString()
        if (action == null) {
            return ctx.tryNextHandler(msg)
        }

        val actionListener = actionListeners[action]
        if (actionListener == null) {
            return ctx.tryNextHandler(msg)
        }

        val dataJson = rootJsonObject?.get("data")
        if (dataJson == null) {
            return ctx.tryNextHandler(msg)
        }
        val response = actionListener.callback(ctx, action, actionListener.parser(dataJson))
        if (response != null) {
            ctx.channel()?.writeAndFlush(TextWebSocketFrame(response))
            return
        }

        ctx.tryNextHandler(msg)
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext?) {
        ctx?.flush()
    }
}
