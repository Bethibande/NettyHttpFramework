package com.bethibande.http.impl.http2.handler

import com.bethibande.http.impl.http2.Http2Connection
import com.bethibande.http.impl.http2.Http2Server
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http2.Http2FrameCodecBuilder
import io.netty.handler.codec.http2.Http2MultiplexHandler
import io.netty.handler.codec.http2.Http2Settings
import io.netty.handler.ssl.ApplicationProtocolNames
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler
import io.netty.util.AttributeKey

class APNHandler(
    private val server: Http2Server,
): ApplicationProtocolNegotiationHandler(ApplicationProtocolNames.HTTP_2) {

    companion object {

        val ATTRIB_SERVER: AttributeKey<Http2Server> = AttributeKey.newInstance("http2_server")
        val ATTRIB_CONNECTION: AttributeKey<Http2Connection> = AttributeKey.newInstance("http2_connection")

    }

    private val streamInitializer = ServerStreamInitializer()

    override fun configurePipeline(p0: ChannelHandlerContext, p1: String) {
        if (ApplicationProtocolNames.HTTP_2 == p1) {
            val codec = Http2FrameCodecBuilder.forServer()
                .initialSettings(Http2Settings.defaultSettings())
                .build()

            val connection = Http2Connection(p0.channel())
            this.server.handleConnection(connection)

            p0.channel().attr(ATTRIB_SERVER).set(this.server)
            p0.channel().attr(ATTRIB_CONNECTION).set(connection)

            p0.pipeline().addLast(
                codec,
                Http2MultiplexHandler(this.streamInitializer),
            )
        }
    }
}