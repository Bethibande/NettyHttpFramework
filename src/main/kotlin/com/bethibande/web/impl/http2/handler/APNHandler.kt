package com.bethibande.web.impl.http2.handler

import com.bethibande.web.impl.http2.Http2Connection
import com.bethibande.web.impl.http2.Http2Server
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http2.Http2FrameCodecBuilder
import io.netty.handler.codec.http2.Http2MultiplexHandler
import io.netty.handler.codec.http2.Http2Settings
import io.netty.handler.ssl.ApplicationProtocolNames
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler

class APNHandler(
    private val server: Http2Server,
): ApplicationProtocolNegotiationHandler(ApplicationProtocolNames.HTTP_2) {

    override fun configurePipeline(p0: ChannelHandlerContext, p1: String) {
        if (ApplicationProtocolNames.HTTP_2 == p1) {
            val codec = Http2FrameCodecBuilder.forServer()
                .initialSettings(Http2Settings.defaultSettings())
                .build()

            val connection = Http2Connection(p0.channel())
            this.server.handleConnection(connection)

            p0.pipeline().addLast(
                Http2FrameCodecBuilder.forServer().build(),
                codec,
                Http2MultiplexHandler(ServerStreamInitializer(this.server, connection)),
            )
        }
    }
}