package com.bethibande.web.impl.http2.handler

import com.bethibande.web.impl.http2.Http2Connection
import com.bethibande.web.impl.http2.context.Http2RequestContext
import com.bethibande.web.request.RequestHook
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.http2.Http2StreamChannel
import io.netty.util.concurrent.Promise

class ClientStreamInitializer(
    private val promise: Promise<Any>,
    private val connection: Http2Connection,
    private val hook: RequestHook,
): ChannelInitializer<Http2StreamChannel>() {

    override fun initChannel(ch: Http2StreamChannel) {
        val context = Http2RequestContext(this.connection, ch, this.promise)

        println("init")

        ch.pipeline().addLast(ClientDataHandler(context))
        this.hook.handle(context)
    }
}