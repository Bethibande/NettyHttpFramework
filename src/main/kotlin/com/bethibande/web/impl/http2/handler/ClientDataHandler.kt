package com.bethibande.web.impl.http2.handler

import com.bethibande.web.impl.http2.context.Http2RequestContext
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http2.Http2DataFrame
import io.netty.handler.codec.http2.Http2HeadersFrame
import io.netty.handler.codec.http2.Http2StreamChannel
import io.netty.handler.codec.http2.Http2StreamFrame

class ClientDataHandler(
    private val context: Http2RequestContext,
): SimpleChannelInboundHandler<Http2StreamFrame>() {

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: Http2StreamFrame) {
        println("received ${msg.javaClass}")
        if(msg is Http2HeadersFrame) {
            this.context.headerCallback(msg.headers())
        }
        if(msg is Http2DataFrame) {
            this.context.dataCallback(msg.content())
        }
    }
}