package com.bethibande.web.impl.http3.handler

import com.bethibande.web.impl.http3.Http3Connection
import com.bethibande.web.impl.http3.Http3Server
import io.netty.channel.ChannelInitializer
import io.netty.incubator.codec.quic.QuicChannel
import io.netty.incubator.codec.quic.QuicStreamType

class ServerConnectionHandler(
    private val server: Http3Server
): ChannelInitializer<QuicChannel>() {

    override fun initChannel(p0: QuicChannel) {
        val connection = Http3Connection(QuicStreamType.UNIDIRECTIONAL, p0)
        this.server.addConnection(connection)

        p0.pipeline().addLast(ServerStreamHandler(connection))
    }

}