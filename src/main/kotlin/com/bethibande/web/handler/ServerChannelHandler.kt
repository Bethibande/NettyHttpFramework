package com.bethibande.web.handler

import com.bethibande.web.Http3Server
import com.bethibande.web.context.HttpResponseContext
import com.bethibande.web.types.QuicConnection
import io.netty.channel.ChannelInitializer
import io.netty.incubator.codec.http3.Http3ServerConnectionHandler
import io.netty.incubator.codec.quic.QuicChannel
import io.netty.incubator.codec.quic.QuicStreamChannel

class ServerChannelHandler(
    private val server: Http3Server
): ChannelInitializer<QuicChannel>() {

    override fun initChannel(p0: QuicChannel) {
        val connection = this.server.connect(p0)
        p0.pipeline().addLast(Http3ServerConnectionHandler(StreamHandler(connection, this.server)))
    }

    class StreamHandler(
        private val connection: QuicConnection,
        private val server: Http3Server
    ): ChannelInitializer<QuicStreamChannel>() {

        override fun initChannel(p0: QuicStreamChannel) {
            val ctx = HttpResponseContext(this.server, this.connection, p0)
            this.server.handle(ctx)

            p0.pipeline().addLast(ResponseRequestHandler(ctx::headerCallback, ctx::dataCallback))
        }

    }

}