package com.bethibande.web.impl.http2.handler

import com.bethibande.web.impl.http2.Http2Server
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.ssl.SslContext

class ServerHandlerInitializer(
    private val sslContext: SslContext,
    private val server: Http2Server,
): ChannelInitializer<SocketChannel>() {

    override fun initChannel(ch: SocketChannel) {
        ch.pipeline().addLast(this.sslContext.newHandler(ch.alloc()), APNHandler(this.server))
    }
}