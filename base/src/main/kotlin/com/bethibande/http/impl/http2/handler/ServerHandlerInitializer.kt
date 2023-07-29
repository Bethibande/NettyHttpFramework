package com.bethibande.http.impl.http2.handler

import com.bethibande.http.impl.http2.Http2Connection
import com.bethibande.http.impl.http2.Http2Server
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http2.Http2FrameCodecBuilder
import io.netty.handler.codec.http2.Http2MultiplexHandler
import io.netty.handler.codec.http2.Http2Settings
import io.netty.handler.ssl.ApplicationProtocolNames
import io.netty.handler.ssl.SslContext
import io.netty.util.AttributeKey

class ServerHandlerInitializer(
    private val sslContext: SslContext?,
    private val server: Http2Server,
): ChannelInitializer<SocketChannel>() {

    companion object {

        val ATTRIB_SERVER: AttributeKey<Http2Server> = AttributeKey.newInstance("http2_server")
        val ATTRIB_CONNECTION: AttributeKey<Http2Connection> = AttributeKey.newInstance("http2_connection")

    }

    private val streamInitializer = ServerStreamInitializer()

    override fun initChannel(ch: SocketChannel) {
        this.sslContext?.let { ch.pipeline().addLast(it.newHandler(ch.alloc())) }
        //ch.pipeline().addLast(APNHandler(this.server))

        val codec = Http2FrameCodecBuilder.forServer()
            .initialSettings(Http2Settings.defaultSettings())
            .build()

        val connection = Http2Connection(ch)
        this.server.handleConnection(connection)

        ch.attr(ATTRIB_SERVER).set(this.server)
        ch.attr(ATTRIB_CONNECTION).set(connection)

        ch.pipeline().addLast(
            codec,
            Http2MultiplexHandler(this.streamInitializer),
        )
    }
}