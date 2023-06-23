package com.bethibande.web.impl.http3.handler

import com.bethibande.web.request.HttpRequestContext
import io.netty.channel.ChannelHandlerContext
import io.netty.incubator.codec.http3.Http3DataFrame
import io.netty.incubator.codec.http3.Http3HeadersFrame
import io.netty.incubator.codec.http3.Http3RequestStreamInboundHandler

class ClientDataHandler<R>: Http3RequestStreamInboundHandler() {

    private lateinit var context: HttpRequestContext<R>

    fun setContext(context: HttpRequestContext<R>) {
        this.context = context
    }

    override fun channelRead(ctx: ChannelHandlerContext, frame: Http3HeadersFrame, isLast: Boolean) {
        context.headerCallback(frame.headers())
    }

    override fun channelRead(ctx: ChannelHandlerContext, frame: Http3DataFrame, isLast: Boolean) {
        context.dataCallback(frame.content())
    }
}