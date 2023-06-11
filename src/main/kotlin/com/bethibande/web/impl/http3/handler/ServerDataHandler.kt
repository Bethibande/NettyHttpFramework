package com.bethibande.web.impl.http3.handler

import com.bethibande.web.impl.http3.Http3Connection
import com.bethibande.web.impl.http3.context.Http3ResponseContext
import io.netty.channel.ChannelHandlerContext
import io.netty.incubator.codec.http3.Http3DataFrame
import io.netty.incubator.codec.http3.Http3HeadersFrame
import io.netty.incubator.codec.http3.Http3RequestStreamInboundHandler

class ServerDataHandler(
    private val connection: Http3Connection,
    private val context: Http3ResponseContext
): Http3RequestStreamInboundHandler() {

    override fun channelRead(p0: ChannelHandlerContext, p1: Http3HeadersFrame, p2: Boolean) {
        context.headerCallback(p1.headers())
    }

    override fun channelRead(p0: ChannelHandlerContext, p1: Http3DataFrame, p2: Boolean) {
        context.dataCallback(p1.content())
    }
}