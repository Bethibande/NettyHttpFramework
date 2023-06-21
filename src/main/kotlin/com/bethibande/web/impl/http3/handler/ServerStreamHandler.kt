package com.bethibande.web.impl.http3.handler

import com.bethibande.web.impl.http3.Http3Connection
import com.bethibande.web.impl.http3.Http3Server
import com.bethibande.web.impl.http3.context.Http3ResponseContext
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.incubator.codec.quic.QuicConnectionEvent
import io.netty.incubator.codec.quic.QuicStreamChannel
import java.net.InetSocketAddress

class ServerStreamHandler(
    private val server: Http3Server,
    private val connection: Http3Connection,
): ChannelInitializer<QuicStreamChannel>() {

    override fun initChannel(p0: QuicStreamChannel) {
        val context = Http3ResponseContext(connection, p0)

        this.connection.addStream(context)
        this.server.handleRequest(context)

        p0.pipeline().addLast(ServerDataHandler(context))
    }

    override fun userEventTriggered(ctx: ChannelHandlerContext?, evt: Any?) {
        if(evt is QuicConnectionEvent) {
            this.connection.updateAddress(evt.newAddress() as InetSocketAddress)
        }

        super.userEventTriggered(ctx, evt)
    }
}