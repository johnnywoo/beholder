package beholder.backend.http

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import java.util.HashMap

Sharable class StaticContentHandler(val resourcesPackageName: String) : SimpleChannelInboundHandler<FullHttpRequest>() {
    override fun channelRead0(ctx: ChannelHandlerContext?, msg: FullHttpRequest?) {
        if (ctx == null) {
            return
        }

        if (msg != null && msg.isSuccess && msg.isMethodGet) {
            val uri     = msg.getUri()?.substringBefore("?") ?: ""
            val fileUri = if (getStaticResource(uri) != null) uri else uri.addUriPathComponent("index.html")

            val resource = getStaticResource(fileUri)
            if (resource != null) {
                ctx.sendHttpResponse(msg, resource.getContent(), HttpResponseStatus.OK, resource.getContentType())
                return
            }
        }

        ctx.tryNextHandler(msg)
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext?) {
        ctx?.flush()
    }

    val staticResources: HashMap<String, StaticResource> = hashMapOf()

    fun getStaticResource(uri: String): StaticResource? {
        if (!staticResources.containsKey(uri)) {
            val resourcePath = "/" + resourcesPackageName.replace(".", "/") + uri // /blah/blah.html -> /beholder/backend/web/blah/blah.html
            if (StaticResource.exists(resourcePath)) {
                staticResources[uri] = StaticResource(resourcePath)
            }
        }
        return staticResources[uri]
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
            // TODO cache this
            return this.javaClass.getResourceAsStream(path)?.use { it.readBytes() } ?: ByteArray(0)
        }
    }
}
