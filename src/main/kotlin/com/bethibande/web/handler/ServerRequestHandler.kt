package com.bethibande.web.handler

import io.netty.channel.ChannelHandlerContext
import io.netty.incubator.codec.http3.Http3DataFrame
import io.netty.incubator.codec.http3.Http3HeadersFrame
import io.netty.incubator.codec.http3.Http3RequestStreamInboundHandler
import java.util.function.BiConsumer

class ServerRequestHandler(
    private val headerConsumer: BiConsumer<Http3HeadersFrame, Boolean>,
    private val dataConsumer: BiConsumer<Http3DataFrame, Boolean>
): Http3RequestStreamInboundHandler() {

    override fun channelRead(p0: ChannelHandlerContext, p1: Http3HeadersFrame, isLast: Boolean) {
        this.headerConsumer.accept(p1, isLast)
    }

    override fun channelRead(p0: ChannelHandlerContext, p1: Http3DataFrame, isLast: Boolean) {
        this.dataConsumer.accept(p1, isLast)
    }

}