package com.bethibande.web.impl.http2.handler

import com.bethibande.web.impl.http2.context.Http2ResponseContext
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http2.Http2DataFrame
import io.netty.handler.codec.http2.Http2HeadersFrame

class ServerDataHandler(
    private val context: Http2ResponseContext?
): ChannelDuplexHandler() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        println(msg.javaClass)
        if (msg is Http2HeadersFrame) {

            return
        }

        if (msg is Http2DataFrame) {

            return
        }

        super.channelRead(ctx, msg)
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

}