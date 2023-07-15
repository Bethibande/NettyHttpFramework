package com.bethibande.http.impl.http2.handler

import com.bethibande.http.impl.http2.context.Http2RequestContext
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http2.Http2DataFrame
import io.netty.handler.codec.http2.Http2HeadersFrame

class ClientDataHandler(
    private val context: Http2RequestContext,
): ChannelDuplexHandler() {

    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any) {
        if(msg is Http2HeadersFrame) {
            this.context.headerCallback(msg.headers())
        }
        if(msg is Http2DataFrame) {
            this.context.dataCallback(msg.content())
        }
    }
}