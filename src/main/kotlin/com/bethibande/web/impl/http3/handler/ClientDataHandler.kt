package com.bethibande.web.impl.http3.handler

import com.bethibande.web.impl.http3.Http3Client
import com.bethibande.web.impl.http3.context.Http3RequestContext
import io.netty.channel.ChannelHandlerContext
import io.netty.incubator.codec.http3.Http3DataFrame
import io.netty.incubator.codec.http3.Http3HeadersFrame
import io.netty.incubator.codec.http3.Http3RequestStreamInboundHandler
import io.netty.incubator.codec.quic.QuicStreamChannel

class ClientDataHandler(
    private val client: Http3Client
): Http3RequestStreamInboundHandler() {

    override fun channelRead(ctx: ChannelHandlerContext, frame: Http3HeadersFrame, isLast: Boolean) {
        context.headerCallback(frame.headers())
    }

    override fun channelRead(ctx: ChannelHandlerContext, frame: Http3DataFrame, isLast: Boolean) {
        context.dataCallback(frame.content())
    }
}