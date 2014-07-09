package beholder.backend

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.SimpleChannelInboundHandler
import java.util.logging.Logger
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import beholder.backend.http.isSuccess
import beholder.backend.http.sendHttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import java.util.logging.Level

Sharable class ErrorHandler : SimpleChannelInboundHandler<Any>() {
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
        logWarning("Exception caught", cause)
        ctx?.close()
    }
}
