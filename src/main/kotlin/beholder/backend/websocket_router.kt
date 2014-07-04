package beholder.backend

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.SimpleChannelInboundHandler
import java.util.logging.Logger
import io.netty.channel.ChannelHandlerContext
import beholder.backend.http.WebsocketHttpHandler
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonElement

Sharable class WebsocketRouter : SimpleChannelInboundHandler<TextWebSocketFrame>() {
    class object {
        val LOGGER      = Logger.getLogger(WebsocketRouter.javaClass.getName())
        val GSON        = Gson()
        val JSON_PARSER = JsonParser()
    }

    private class ActionListener(val parser: (JsonElement) -> Any, val callback: (ChannelHandlerContext, String, Any) -> String?)

    private val actionListeners: MutableMap<String, ActionListener> = hashMapOf()

    fun onAction<T>(action: String, clazz: Class<T>, callback: (ctx: ChannelHandlerContext, action: String, data: Any) -> String?) {
        actionListeners.put(action, ActionListener({ GSON.fromJson(it, clazz) as Any }, callback))
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: TextWebSocketFrame?) {
        val handshaker = ctx!!.channel()?.attr(WebsocketHttpHandler.CHANNEL_ATTR_HANDSHAKER)?.get()
        if (handshaker == null) {
            throw RuntimeException("No handshaker for incoming websocket frame")
        }

        if (msg == null) {
            ctx.fireChannelRead(msg?.retain())
            return
        }

        val jsonElement = JSON_PARSER.parse(msg.text())
        if (jsonElement == null) {
            ctx.fireChannelRead(msg.retain())
            return
        }
        if (!jsonElement.isJsonObject()) {
            ctx.fireChannelRead(msg.retain())
            return
        }
        val rootJsonObject = jsonElement.getAsJsonObject()
        if (rootJsonObject == null) {
            ctx.fireChannelRead(msg.retain())
            return
        }
        val action = rootJsonObject.get("action")?.getAsString()
        if (action == null) {
            ctx.fireChannelRead(msg.retain())
            return
        }
        if (!actionListeners.contains(action)) {
            ctx.fireChannelRead(msg.retain())
            return
        }

        val pair = actionListeners[action]
        if (pair == null) {
            ctx.fireChannelRead(msg.retain())
            return
        }
        val dataJson = rootJsonObject.get("data")
        if (dataJson == null) {
            ctx.fireChannelRead(msg.retain())
            return
        }
        val response = pair.callback(ctx, action, pair.parser(dataJson))
        if (response != null) {
            ctx.channel()?.writeAndFlush(TextWebSocketFrame(response))
        }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext?) {
        ctx?.flush()
    }
}

class WebsocketRequest {
    var action: String? = null
    var data: Any? = null
}
