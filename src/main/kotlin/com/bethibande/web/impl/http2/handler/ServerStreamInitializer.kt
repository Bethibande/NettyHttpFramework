package com.bethibande.web.impl.http2.handler

import com.bethibande.web.impl.http2.Http2Server
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.http2.Http2StreamChannel

class ServerStreamInitializer(
    private val server: Http2Server
): ChannelInitializer<Http2StreamChannel>() {

    override fun initChannel(ch: Http2StreamChannel) {
        println("initialize: $ch")
    }
}