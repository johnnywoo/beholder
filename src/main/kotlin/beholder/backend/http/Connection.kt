package beholder.backend.http

import io.netty.channel.ChannelHandlerContext
import io.netty.util.AttributeKey
import beholder.backend.config.UserConfiguration
import io.netty.channel.group.DefaultChannelGroup
import io.netty.util.concurrent.GlobalEventExecutor
import beholder.backend.api.Message
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import beholder.backend.gson

class Connection(val ctx: ChannelHandlerContext) {
    class object {
        val channelAttrUserConfiguration: AttributeKey<UserConfiguration>? = AttributeKey.valueOf("userConfiguration")
        val clientChannelGroup = DefaultChannelGroup(GlobalEventExecutor.INSTANCE)
    }

    fun isAuthorized()
        = user != null

    var user: UserConfiguration?
        get() = ctx.attr(channelAttrUserConfiguration)?.get()
        set(userConfiguration: UserConfiguration?) {
            ctx.attr(channelAttrUserConfiguration)?.set(userConfiguration)
            val channel = ctx.channel()
            if (channel != null) {
                clientChannelGroup.add(channel)
            }
        }

    fun send(message: Message) {
        ctx.channel()?.writeAndFlush(TextWebSocketFrame(gson.toJson(message)))
    }
}
