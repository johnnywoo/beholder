package beholder.backend.http

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpHeaders.Names
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpVersion
import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil
import io.netty.handler.codec.http.HttpHeaders
import io.netty.channel.ChannelFutureListener
import sun.misc.BASE64Decoder

import beholder.backend.configuration
import io.netty.util.AttributeKey
import beholder.backend.config.UserConfiguration

Sharable class BasicAuthHandler : SimpleChannelInboundHandler<FullHttpRequest>() {
    class object {
        val channelAttrApiKey: AttributeKey<String>? = AttributeKey.valueOf("apiKey")
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: FullHttpRequest?) {
        if (ctx == null) {
            return
        }

        if (msg != null && msg.isSuccess) {
            val header = msg.headers()?.get(Names.AUTHORIZATION)
            if (header != null && header.startsWith("Basic ")) {
                val base64 = header.substring("Basic ".length)
                val auth = String(BASE64Decoder().decodeBuffer(base64) ?: ByteArray(0), defaultCharset)

                val login    = auth.substringBefore(":")
                val password = auth.substringAfter(":")

                val user = configuration.getUserConfiguration(login)
                if (user != null && user.password == password) {
                    ctx.attr(channelAttrApiKey)?.set(user.apiKey)
                    ctx.tryNextHandler(msg)
                    return
                }
            }
        }

        // responding with 401 and a request for basic auth
        val response = DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.UNAUTHORIZED,
            Unpooled.copiedBuffer(HttpResponseStatus.UNAUTHORIZED.toString(), CharsetUtil.UTF_8)
        )

        // send the response and close the connection if necessary
        HttpHeaders.setContentLength(response, response.contentLength)
        response.headers()?.set(Names.WWW_AUTHENTICATE, "Basic realm=\"Beholder\"")
        response.headers()?.set(Names.CONTENT_TYPE, "text/plain")
        val writeFuture = ctx.channel()?.writeAndFlush(response)
        writeFuture?.addListener(ChannelFutureListener.CLOSE)
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext?) {
        ctx?.flush()
    }
}
