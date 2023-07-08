package com.bethibande.web.impl.http2.handler

import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.http2.Http2FrameCodecBuilder
import io.netty.handler.codec.http2.Http2Settings
import io.netty.handler.ssl.SslContext

class ClientHandlerInitializer(
    private val sslContext: SslContext,
): ChannelInitializer<Channel>() {

    override fun initChannel(ch: Channel) {
        ch.pipeline().addFirst(this.sslContext.newHandler(ch.alloc()))

        val frameCodec = Http2FrameCodecBuilder.forClient()
            .initialSettings(Http2Settings.defaultSettings())
            .build()

        ch.pipeline().addLast(frameCodec)
    }
}