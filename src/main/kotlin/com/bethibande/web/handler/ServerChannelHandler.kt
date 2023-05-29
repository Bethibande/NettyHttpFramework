package com.bethibande.web.handler

import com.bethibande.web.Http3Server
import com.bethibande.web.context.HttpServerContext
import io.netty.channel.ChannelInitializer
import io.netty.incubator.codec.http3.Http3ServerConnectionHandler
import io.netty.incubator.codec.quic.QuicChannel
import io.netty.incubator.codec.quic.QuicStreamChannel

class ServerChannelHandler(
    private val server: Http3Server
): ChannelInitializer<QuicChannel>() {

    override fun initChannel(p0: QuicChannel) {
        p0.pipeline().addLast(Http3ServerConnectionHandler(StreamHandler(this.server)))
    }

    class StreamHandler(
        private val server: Http3Server
    ): ChannelInitializer<QuicStreamChannel>() {

        override fun initChannel(p0: QuicStreamChannel) {
            val ctx = HttpServerContext(this.server, p0)
            this.server.handle(ctx)

            p0.pipeline().addLast(ServerRequestHandler(ctx::headerCallback, ctx::dataCallback))
        }

    }

}