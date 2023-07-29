package com.bethibande.http.impl.http2.handler

import io.netty.channel.*
import io.netty.handler.codec.http2.Http2FrameCodecBuilder
import io.netty.handler.codec.http2.Http2MultiplexHandler
import io.netty.handler.codec.http2.Http2Settings
import io.netty.handler.ssl.SslContext

class ClientHandlerInitializer(
    private val sslContext: SslContext?,
): ChannelInitializer<Channel>() {

    override fun initChannel(ch: Channel) {
        this.sslContext?.let { ch.pipeline().addFirst(it.newHandler(ch.alloc())) }

        val frameCodec = Http2FrameCodecBuilder.forClient()
            .initialSettings(Http2Settings.defaultSettings())
            .build()

        val multiplexHandler = Http2MultiplexHandler(object: SimpleChannelInboundHandler<Any>() {
            override fun channelRead0(ctx: ChannelHandlerContext, msg: Any) { }
        })

        ch.pipeline().addLast(frameCodec)
        ch.pipeline().addLast(multiplexHandler)
    }
}