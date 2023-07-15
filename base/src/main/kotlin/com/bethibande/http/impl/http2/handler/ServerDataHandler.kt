package com.bethibande.http.impl.http2.handler

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http2.Http2DataFrame
import io.netty.handler.codec.http2.Http2HeadersFrame

@Sharable
class ServerDataHandler: ChannelDuplexHandler() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val context = ctx.channel().attr(ServerStreamInitializer.ATTRIB_CONTEXT).get()

        if (msg is Http2HeadersFrame) {
            context.headerCallback(msg.headers())
            return
        }

        if (msg is Http2DataFrame) {
            context.dataCallback(msg.content())
            return
        }
    }
}