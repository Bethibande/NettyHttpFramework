package com.bethibande.web.impl.http3.handler

import com.bethibande.web.impl.http3.Http3Connection
import com.bethibande.web.impl.http3.context.Http3ResponseContext
import io.netty.channel.ChannelInitializer
import io.netty.incubator.codec.quic.QuicStreamChannel

class ServerStreamHandler(
    private val connection: Http3Connection
): ChannelInitializer<QuicStreamChannel>() {

    override fun initChannel(p0: QuicStreamChannel) {
        val context = Http3ResponseContext(connection, p0)
        this.connection.addStream(context)
        p0.pipeline().addLast(ServerDataHandler(this.connection, context))
    }
}