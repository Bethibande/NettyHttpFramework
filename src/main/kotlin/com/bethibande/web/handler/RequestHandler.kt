package com.bethibande.web.handler

import com.bethibande.web.Http3Server
import com.bethibande.web.context.HttpServerContext
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpMethod
import io.netty.incubator.codec.http3.Http3DataFrame
import io.netty.incubator.codec.http3.Http3HeadersFrame
import io.netty.incubator.codec.http3.Http3RequestStreamInboundHandler
import io.netty.util.AttributeKey

class RequestHandler(
    private val server: Http3Server
): Http3RequestStreamInboundHandler() {

    companion object {
        val ATTRIBUTE_HTTP_CONTEXT = AttributeKey.valueOf<HttpServerContext>("HTTP_CONTEXT")!!
    }

    override fun channelRead(p0: ChannelHandlerContext, p1: Http3HeadersFrame, isLast: Boolean) {
        val headers = p1.headers()
        val attr = p0.channel().attr(ATTRIBUTE_HTTP_CONTEXT)

        attr.set(HttpServerContext(
            headers.path().toString(),
            HttpMethod.valueOf(headers.method().toString()),
            null,
            headers,
        ).withContext(p0))

        this.server.routes { this.process(attr.get()) }
    }

    override fun channelRead(p0: ChannelHandlerContext, p1: Http3DataFrame, isLast: Boolean) {
        val context = p0.channel().attr(ATTRIBUTE_HTTP_CONTEXT).get()

        context?.let {
            it.withContext(p0).accept(p1, isLast)
            p1.content().release()
        }
    }

}