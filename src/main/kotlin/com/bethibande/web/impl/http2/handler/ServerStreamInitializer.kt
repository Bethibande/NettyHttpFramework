package com.bethibande.web.impl.http2.handler

import com.bethibande.web.impl.http2.Http2Connection
import com.bethibande.web.impl.http2.Http2Server
import com.bethibande.web.impl.http2.context.Http2ResponseContext
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.http2.Http2StreamChannel

class ServerStreamInitializer(
    private val server: Http2Server,
    private val connection: Http2Connection,
): ChannelInitializer<Http2StreamChannel>() {

    override fun initChannel(ch: Http2StreamChannel) {
        val context = Http2ResponseContext(
            this.connection,
            ch
        )

        ch.pipeline().addLast(ServerDataHandler(context))

        this.server.handleRequest(context)
    }
}