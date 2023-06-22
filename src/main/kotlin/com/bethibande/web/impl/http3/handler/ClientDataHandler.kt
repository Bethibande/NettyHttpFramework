package com.bethibande.web.impl.http3.handler

import com.bethibande.web.impl.http3.Http3Client
import com.bethibande.web.impl.http3.context.Http3RequestContext
import com.bethibande.web.request.RequestHook
import io.netty.channel.ChannelHandlerContext
import io.netty.incubator.codec.http3.Http3DataFrame
import io.netty.incubator.codec.http3.Http3HeadersFrame
import io.netty.incubator.codec.http3.Http3RequestStreamInboundHandler
import io.netty.incubator.codec.quic.QuicConnectionEvent
import io.netty.incubator.codec.quic.QuicStreamChannel
import io.netty.util.concurrent.Promise

class ClientDataHandler<R>(
    private val client: Http3Client,
    private val contextCallback: RequestHook<R>,
    private val future: Promise<R>
): Http3RequestStreamInboundHandler() {

    private var context: Http3RequestContext<R>? = null

    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any?) {
        if(context == null) {
            context = Http3RequestContext(client.connection(), ctx.channel() as QuicStreamChannel, this.future)
            this.contextCallback.handle(context!!)
        }

        super.userEventTriggered(ctx, evt)
    }

    override fun channelRead(ctx: ChannelHandlerContext, frame: Http3HeadersFrame, isLast: Boolean) {
        context!!.headerCallback(frame.headers())
    }

    override fun channelRead(ctx: ChannelHandlerContext, frame: Http3DataFrame, isLast: Boolean) {
        context!!.dataCallback(frame.content())
    }
}