package com.bethibande.web.impl.http2.handler

import com.bethibande.web.impl.http2.Http2Connection
import com.bethibande.web.impl.http2.context.Http2ResponseContext
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.handler.codec.http2.Http2DataFrame
import io.netty.handler.codec.http2.Http2HeadersFrame
import io.netty.handler.codec.http2.Http2StreamChannel

class ServerDataHandler(
    private val context: Http2ResponseContext
): ChannelDuplexHandler() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        println((ctx.channel() as Http2StreamChannel).stream().id())

        if (msg is Http2HeadersFrame) {
            println("received server: ${msg.javaClass}")
            this.context.headerCallback(msg.headers())
            return
        }

        if (msg is Http2DataFrame) {
            println("received server: ${msg.javaClass}")
            this.context.dataCallback(msg.content())
            return
        }

        super.channelRead(ctx, msg)
    }

    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        println("write server: ${msg.javaClass}")
        super.write(ctx, msg, promise)
        println((ctx.channel() as Http2StreamChannel).stream().id())
    }
}