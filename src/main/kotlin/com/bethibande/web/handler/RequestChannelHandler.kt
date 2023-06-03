package com.bethibande.web.handler

import io.netty.channel.ChannelHandlerContext
import io.netty.incubator.codec.http3.Http3DataFrame
import io.netty.incubator.codec.http3.Http3HeadersFrame
import io.netty.incubator.codec.http3.Http3RequestStreamInboundHandler
import java.util.function.BiConsumer

class RequestChannelHandler(
    private val headerCallback: BiConsumer<Http3HeadersFrame, Boolean>,
    private val dataCallback: BiConsumer<Http3DataFrame, Boolean>
): Http3RequestStreamInboundHandler() {

    override fun channelRead(p0: ChannelHandlerContext?, p1: Http3DataFrame, p2: Boolean) {
        this.dataCallback.accept(p1, p2)
    }

    override fun channelRead(p0: ChannelHandlerContext?, p1: Http3HeadersFrame, p2: Boolean) {
        this.headerCallback.accept(p1, p2)
    }

}